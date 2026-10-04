package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;

@IntegrationTest
@Import(LecturesCoherentesDuSuiviIT.ConfigurationDesRendezVous.class)
class LecturesCoherentesDuSuiviIT {

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private SuivisDAtelierApplicationService application;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private RendezVousDesLectures rendezVous;

  @Autowired
  private EntityManager entities;

  @Test
  @WithTenant("impeccmold")
  void shouldLireUneRevisionUnJournalEtUneClotureDuMemeInstantane() throws Exception {
    // GIVEN
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var ancien = inTransaction(() -> suivis.create(suiviDAtelierEngage().enregistre(debut)));
    var journalDemandee = new CountDownLatch(1);
    var ecritureTerminee = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var lecture = executor.submit(
        dansContexteCourant(() -> {
          rendezVous.suspendAvantJournal(journalDemandee, ecritureTerminee);
          try {
            return application.get(ancien.id()).suivi();
          } finally {
            rendezVous.libere();
          }
        })
      );
      try {
        attend(journalDemandee);
        // WHEN
        var nouveau = inTransaction(() ->
          suivis.update(
            ancien
              .annule(debut.id(), new Annulation(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, MOTIF_ERREUR_DE_SAISIE))
              .cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))
          )
        );
        ecritureTerminee.countDown();
        // THEN
        assertThat(lecture.get(10, TimeUnit.SECONDS)).isIn(ancien, nouveau);
      } finally {
        ecritureTerminee.countDown();
      }
    }
  }

  @Test
  @WithTenant("impeccmold")
  void shouldLireUnSuiviCoherentMalgreLeParentDejaChargeParLAppelant() {
    // GIVEN
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var ancien = inTransaction(() -> suivis.create(suiviDAtelierEngage().enregistre(debut)));
    // WHEN / THEN
    avecParentDejaChargeEtAnnulationClotureCommittees(ancien, nouveau ->
      assertThat(application.get(ancien.id()).suivi()).isIn(ancien, nouveau)
    );
  }

  private void avecParentDejaChargeEtAnnulationClotureCommittees(SuiviDAtelier ancien, Consumer<SuiviDAtelier> lecture) {
    try (var executor = Executors.newSingleThreadExecutor()) {
      inTransaction(() -> {
        entities.find(SuiviDAtelierEntity.class, ancien.id().uuid());
        var ecriture = executor.submit(
          dansContexteCourant(() ->
            inTransaction(() ->
              suivis.update(
                ancien
                  .annule(
                    ancien.journal().evenements().getFirst().id(),
                    new Annulation(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, MOTIF_ERREUR_DE_SAISIE)
                  )
                  .cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))
              )
            )
          )
        );
        lecture.accept(attendResultat(ecriture));
        return null;
      });
    }
  }

  private static <T> Callable<T> dansContexteCourant(Supplier<T> action) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var requete = RequestContextHolder.getRequestAttributes();
    return () -> {
      var contexte = SecurityContextHolder.createEmptyContext();
      contexte.setAuthentication(authentication);
      SecurityContextHolder.setContext(contexte);
      RequestContextHolder.setRequestAttributes(requete);
      try {
        return action.get();
      } finally {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
      }
    };
  }

  private static <T> T attendResultat(Future<T> resultat) {
    try {
      return resultat.get(10, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  private static void attend(CountDownLatch signal) {
    try {
      assertThat(signal.await(10, TimeUnit.SECONDS)).as("La transaction rejoint son rendez-vous").isTrue();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ConfigurationDesRendezVous {

    @Bean
    RendezVousDesLectures rendezVousDesLectures() {
      return new RendezVousDesLectures();
    }

    @Bean
    HibernatePropertiesCustomizer inspectionDesLectures(RendezVousDesLectures rendezVous) {
      return properties -> properties.put("hibernate.session_factory.statement_inspector", rendezVous);
    }
  }

  static final class RendezVousDesLectures implements StatementInspector {

    private final ThreadLocal<Rencontre> rencontre = new ThreadLocal<>();

    void suspendAvantJournal(CountDownLatch journalDemandee, CountDownLatch ecritureTerminee) {
      rencontre.set(new Rencontre(journalDemandee, ecritureTerminee));
    }

    void libere() {
      rencontre.remove();
    }

    @Override
    public String inspect(String sql) {
      var attendue = rencontre.get();
      if (attendue != null && sql.contains("evenement_d_atelier")) {
        rencontre.remove();
        attendue.journalDemandee().countDown();
        attend(attendue.ecritureTerminee());
      }
      return sql;
    }
  }

  private record Rencontre(CountDownLatch journalDemandee, CountDownLatch ecritureTerminee) {}
}

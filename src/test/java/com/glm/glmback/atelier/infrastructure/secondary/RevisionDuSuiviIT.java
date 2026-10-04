package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;

@IntegrationTest
class RevisionDuSuiviIT {

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneClotureQuiPerdraitUneAnnulationConcurrente() throws Exception {
    // GIVEN
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = inTransaction(() -> suivis.create(suiviDAtelierEngage().enregistre(debut)));
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var requete = RequestContextHolder.getRequestAttributes();
    var lecture = new CountDownLatch(1);
    var annulation = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var ancienneCloture = executor.submit(() -> {
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(authentication);
        SecurityContextHolder.setContext(contexte);
        RequestContextHolder.setRequestAttributes(requete);
        try {
          return catchThrowable(() ->
            inTransaction(() -> {
              SuiviDAtelier ancien = suivis.get(suivi.id()).orElseThrow();
              lecture.countDown();
              attend(annulation);
              return suivis.update(ancien.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H)));
            })
          );
        } finally {
          SecurityContextHolder.clearContext();
          RequestContextHolder.resetRequestAttributes();
        }
      });
      try {
        attend(lecture);
        // WHEN
        inTransaction(() ->
          suivis.update(suivi.annule(debut.id(), new Annulation(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, MOTIF_ERREUR_DE_SAISIE)))
        );
      } finally {
        annulation.countDown();
      }
      // THEN
      assertThat(ancienneCloture.get(10, TimeUnit.SECONDS)).isExactlyInstanceOf(SaisieConcurrenteException.class);
      var relu = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
      assertThat(relu.journal().actifs()).isEmpty();
      assertThat(relu.cloture()).isEmpty();
    }
  }

  @Test
  @WithTenant("impeccmold")
  void shouldConserverLaRevisionQuandAucunFaitNiClotureNeChange() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviDAtelierEngage()));
    var versionLue = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
    // WHEN
    var inchange = inTransaction(() -> suivis.update(versionLue));
    // THEN
    assertThat(inchange.revision()).isEqualTo(versionLue.revision());
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(versionLue);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireLeJournalSousVerrouApresUneLectureDevenuePerimee() throws Exception {
    // GIVEN
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = inTransaction(() -> suivis.create(suiviDAtelierEngage().enregistre(debut)));
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var requete = RequestContextHolder.getRequestAttributes();
    var lecture = new CountDownLatch(1);
    var annulation = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var relecture = executor.submit(() -> {
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(authentication);
        SecurityContextHolder.setContext(contexte);
        RequestContextHolder.setRequestAttributes(requete);
        try {
          return inTransaction(() -> {
            suivis.get(suivi.id()).orElseThrow();
            lecture.countDown();
            attend(annulation);
            return suivis.getForUpdate(suivi.id()).orElseThrow();
          });
        } finally {
          SecurityContextHolder.clearContext();
          RequestContextHolder.resetRequestAttributes();
        }
      });
      try {
        attend(lecture);
        // WHEN
        inTransaction(() ->
          suivis.update(suivi.annule(debut.id(), new Annulation(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, MOTIF_ERREUR_DE_SAISIE)))
        );
      } finally {
        annulation.countDown();
      }
      // THEN
      var relu = relecture.get(10, TimeUnit.SECONDS);
      assertThat(relu.journal().actifs()).isEmpty();
      assertThat(relu.revision().value()).isEqualTo(1);
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
}

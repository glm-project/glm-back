package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.ConfirmerLesActes;
import com.glm.glmback.atelier.application.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.PreparationDesActes;
import com.glm.glmback.atelier.application.RecusDActes;
import com.glm.glmback.atelier.application.ReferencesDApercu;
import com.glm.glmback.atelier.domain.ConfirmationReutiliseeException;
import com.glm.glmback.atelier.domain.ApercuInvalideException;
import com.glm.glmback.atelier.domain.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.function.Supplier;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

@IntegrationTest
@Import(ConfirmationsDActesIT.Configuration.class)
class ConfirmationsDActesIT {

  @Autowired
  private ConfirmerLesActes confirmations;

  @Autowired
  private RecusDActes recus;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @MockitoBean
  private ReferencesDApercu references;

  @MockitoBean
  private EmpreintesDesConsequences empreintes;

  @MockitoBean
  private Clock clock;

  @BeforeEach
  void evaluation() {
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    when(empreintes.calcule(any(), any())).thenReturn("consequences-annulation");
  }

  @Test
  @WithTenant("impeccmold")
  void shouldEnregistrerLAnnulationEtSonRecuDansLaMemeTransaction() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    // WHEN
    var resultat = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    // THEN
    assertThat(resultat).isNotNull();
    assertThat(resultat.recu().preuve()).isEqualTo(preuve);
    assertThat(resultat.recu().revisionEnregistree().value()).isEqualTo(1);
    assertThat(resultat.recu().evenementsTouches()).containsExactly(preuve.adresse().pointage());
    assertThat(resultat.dossier().lecture().suivi().conflits()).isEmpty();
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).contains(resultat.recu());
    assertThat(
      inTransaction(() -> suivis.get(suivi.id()))
        .orElseThrow()
        .journal()
        .evenement(preuve.adresse().pointage())
        .orElseThrow()
        .annulation()
    ).isPresent();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejouerLaMemeConfirmationSansEnregistrerUnSecondActe() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    var premier = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN
    var rejeu = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    // THEN
    assertThat(rejeu.recu()).isEqualTo(premier.recu());
    assertThat(rejeu.dossier()).isEqualTo(premier.dossier());
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneAutreReferencePourLaMemeCommande() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    var premier = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-modifiee", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ConfirmationReutiliseeException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnAutreSujetQuiPorteLeMemeNomDAuteur() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    var premier = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_MARTIN_IMPECCMOLD)
    ).isExactlyInstanceOf(ConfirmationReutiliseeException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserLeRejeuSurUnAutreSuivi() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var autre = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    var premier = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(autre.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ConfirmationReutiliseeException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
    assertThat(inTransaction(() -> suivis.get(autre.id()))).contains(autre);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnePreuvePrepareeParUnAutreSujet() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_MARTIN_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuInvalideException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneCommandeQuiNeCorrespondPasALaPreuve() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    var autreCommande = UUID.randomUUID();
    when(references.read("reference-annulation")).thenReturn(preuve);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), autreCommande, "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuInvalideException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
    assertThat(inTransaction(() -> recus.get(autreCommande))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnePreuveDestineeAUnAutreSuivi() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var autre = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(autre.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuInvalideException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> suivis.get(autre.id()))).contains(autre);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnApercuAnterieurAUneCloture() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    var cloture = inTransaction(() -> suivis.update(suivi.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))));
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(cloture);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnApercuALInstantDeSonExpiration() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    when(clock.now()).thenReturn(preuve.expireLe());
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserDesConsequencesDevenuesDifferentesSansChangementDeRevision() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    when(empreintes.calcule(any(), any())).thenReturn("consequences-modifiees");
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejouerDeuxConfirmationsSimultaneesDeLaMemeCommande() throws Exception {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    var lecturesSansRecu = new CountDownLatch(2);
    when(references.read("reference-annulation")).thenAnswer(invocation -> {
      lecturesSansRecu.countDown();
      assertThat(lecturesSansRecu.await(10, TimeUnit.SECONDS)).as("Les deux requetes ont lu l absence du recu").isTrue();
      return preuve;
    });
    var action = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD))
    );
    // WHEN THEN
    try (var executor = Executors.newFixedThreadPool(2)) {
      var premier = executor.submit(action::get);
      var second = executor.submit(action::get);
      assertThat(premier.get(15, TimeUnit.SECONDS)).isNull();
      assertThat(second.get(15, TimeUnit.SECONDS)).isNull();
    }
    var relu = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
    assertThat(relu.revision().value()).isEqualTo(1);
    assertThat(relu.journal().evenements()).hasSize(2);
    assertThat(relu.journal().evenement(preuve.adresse().pointage()).orElseThrow().annulation()).isPresent();
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isPresent();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRetrouverLActeApresUneReponsePerdueEtRelireLeSuiviActuel() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    var avantCloture = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
    var cloture = inTransaction(() -> suivis.update(avantCloture.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))));
    // WHEN
    var resultat = confirmations.verifier(suivi.id(), preuve.commande(), CONTEXTE_LEROY_IMPECCMOLD);
    // THEN
    assertThat(resultat)
      .get()
      .satisfies(atteste -> {
        assertThat(atteste.recu().preuve()).isEqualTo(preuve);
        assertThat(atteste.recu().revisionEnregistree().value()).isEqualTo(1);
        assertThat(atteste.dossier().lecture().suivi()).isEqualTo(cloture);
        assertThat(atteste.dossier().lecture().suivi().revision().value()).isEqualTo(2);
        assertThat(atteste.dossier().activites()).hasSize(1);
      });
  }

  @Test
  @WithTenant("impeccmold")
  void shouldGarderLAbsenceDeRecuNonConcluantePendantUneConfirmationEnCours() throws Exception {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    var preparationCommencee = new CountDownLatch(1);
    var termineLaPreparation = new CountDownLatch(1);
    when(references.read("reference-annulation")).thenAnswer(invocation -> {
      preparationCommencee.countDown();
      assertThat(termineLaPreparation.await(10, TimeUnit.SECONDS)).isTrue();
      return preuve;
    });
    var action = avecContexteDeRequete(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    );
    try (var executor = Executors.newSingleThreadExecutor()) {
      var premiereRequete = executor.submit(action::get);
      try {
        assertThat(preparationCommencee.await(10, TimeUnit.SECONDS)).isTrue();
        // WHEN THEN
        assertThat(confirmations.verifier(suivi.id(), preuve.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isEmpty();
      } finally {
        termineLaPreparation.countDown();
      }
      assertThat(premiereRequete.get(15, TimeUnit.SECONDS)).isNotNull();
    }
    assertThat(confirmations.verifier(suivi.id(), preuve.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isPresent();
  }

  private static <T> Supplier<T> avecContexteDeRequete(Supplier<T> action) {
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

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }

  @TestConfiguration
  static class Configuration {

    @Bean
    PreparationDesActes preparation(
      SuiviDAtelierRepository suivis,
      ElementsEngageables elements,
      OperateursConnus operateurs,
      PostesConnus postes,
      Habilitations habilitations,
      EmpreintesDesConsequences empreintes
    ) {
      return PreparationDesActes.builder()
        .repository(suivis)
        .elements(elements)
        .operateurs(operateurs)
        .postes(postes)
        .habilitations(habilitations)
        .empreintes(empreintes);
    }

    @Bean
    ConfirmerLesActes confirmations(
      SuiviDAtelierRepository suivis,
      RecusDActes recus,
      ReferencesDApercu references,
      PreparationDesActes preparation,
      Clock clock
    ) {
      return ConfirmerLesActes.builder().suivis(suivis).recus(recus).references(references).preparation(preparation).clock(clock);
    }
  }
}

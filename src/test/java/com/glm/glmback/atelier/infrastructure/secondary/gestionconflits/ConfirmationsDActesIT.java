package com.glm.glmback.atelier.infrastructure.secondary.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.application.gestionconflits.ConfirmerLesActes;
import com.glm.glmback.atelier.application.gestionconflits.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.gestionconflits.PreparationDesActes;
import com.glm.glmback.atelier.application.gestionconflits.PropositionAConfirmer;
import com.glm.glmback.atelier.application.gestionconflits.RecusDActes;
import com.glm.glmback.atelier.domain.ClotureAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.gestionconflits.ActeDeResolution;
import com.glm.glmback.atelier.domain.gestionconflits.AdresseDossierConflit;
import com.glm.glmback.atelier.domain.gestionconflits.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.gestionconflits.ConfirmationReutiliseeException;
import com.glm.glmback.atelier.domain.gestionconflits.EtatDAdresseDossier;
import com.glm.glmback.atelier.domain.gestionconflits.PropositionInvalideException;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;

@IntegrationTest
class ConfirmationsDActesIT {

  @Autowired
  private ConfirmerLesActes confirmations;

  @Autowired
  private PreparationDesActes preparation;

  @Autowired
  private SuivisDAtelierApplicationService atelier;

  @MockitoSpyBean
  private RecusDActes recus;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entities;

  @Autowired
  private IdentitesDEvenements identites;

  @MockitoBean
  private EmpreintesDesConsequences empreintes;

  @MockitoBean
  private Clock clock;

  @MockitoBean
  private OperateursConnus operateurs;

  @MockitoBean
  private PostesConnus postes;

  @MockitoBean
  private Habilitations habilitations;

  @BeforeEach
  void evaluation() {
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    when(empreintes.calcule(any(), any())).thenReturn("consequences-annulation");
    when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
    when(operateurs.get(OPERATEUR_ID_MARTIN)).thenReturn(Optional.of(OPERATEUR_CONNU_MARTIN));
    when(postes.get(POSTE_ID_FRAISEUSE_1)).thenReturn(Optional.of(POSTE_CONNU_FRAISEUSE_1));
    when(habilitations.estHabilite(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1)).thenReturn(true);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldEnregistrerLAnnulationEtSonRecuDansLaMemeTransaction() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    // WHEN
    var resultat = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    // THEN
    assertThat(resultat).isNotNull();
    assertThat(resultat.recu().proposition()).isEqualTo(proposition);
    assertThat(resultat.recu().revisionEnregistree().value()).isEqualTo(1);
    assertThat(resultat.recu().evenementsTouches()).containsExactly(proposition.adresse().pointage());
    assertThat(resultat.dossier().lecture().suivi().conflits()).isEmpty();
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).contains(resultat.recu());
    assertThat(
      inTransaction(() -> suivis.get(suivi.id()))
        .orElseThrow()
        .journal()
        .evenement(proposition.adresse().pointage())
        .orElseThrow()
        .annulation()
    ).isPresent();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejouerLaMemeConfirmationSansEnregistrerUnSecondActe() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var premier = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN
    var rejeu = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    // THEN
    assertThat(rejeu.recu()).isEqualTo(premier.recu());
    assertThat(rejeu.dossier()).isEqualTo(premier.dossier());
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneAutrePropositionPourLaMemeCommande() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var premier = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), ConcurrenceDesActesFixture.avecEmpreinte(proposition), CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ConfirmationReutiliseeException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
  }

  @ParameterizedTest
  @EnumSource(ReprisesDActesFixture.CasDeRejeu.class)
  @WithTenant("impeccmold")
  void shouldRefuserLeRejeuDontUnChampMetierDeLActeAChange(ReprisesDActesFixture.CasDeRejeu cas) {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var originale = cas.originale(suivi);
    var modifiee = cas.modifiee(new ReprisesDActesFixture.DemandeSurSuivi(originale, suivi));
    assertThatCode(() -> preparation.prepare(suivi, modifiee.acte(), modifiee.evenement(), AUTEUR_LEROY, LE_10_MAI_2026_A_17H))
      .as("La demande modifiee est un acte metier valide sur le suivi initial")
      .doesNotThrowAnyException();
    var premier = confirmations.confirmer(suivi.id(), originale, CONTEXTE_LEROY_IMPECCMOLD);
    var suiviConfirme = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), modifiee, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      ConfirmationReutiliseeException.class
    );
    var relu = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
    assertThat(relu.journal()).isEqualTo(suiviConfirme.journal());
    assertThat(relu.revision()).isEqualTo(suiviConfirme.revision());
    assertThat(inTransaction(() -> recus.get(originale.commande()))).contains(premier.recu());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnAutreSujetQuiPorteLeMemeNomDAuteur() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var premier = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_MARTIN_IMPECCMOLD)).isExactlyInstanceOf(
      ConfirmationReutiliseeException.class
    );
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserLeRejeuSurUnAutreSuivi() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var autre = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var premier = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(autre.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      PropositionInvalideException.class
    );
    assertThatThrownBy(() -> confirmations.verifier(autre.id(), proposition.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      ConfirmationReutiliseeException.class
    );
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(premier.dossier().lecture().suivi());
    assertThat(inTransaction(() -> suivis.get(autre.id()))).contains(autre);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnePropositionDestineeAUnAutreSuivi() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var autre = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(autre.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      PropositionInvalideException.class
    );
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> suivis.get(autre.id()))).contains(autre);
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnApercuAnterieurAUneCloture() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var cloture = inTransaction(() -> suivis.update(suivi.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))));
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      ApercuObsoleteException.class
    );
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(cloture);
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserDesConsequencesDevenuesDifferentesSansChangementDeRevision() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    when(empreintes.calcule(any(), any())).thenReturn("consequences-modifiees");
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      ApercuObsoleteException.class
    );
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejouerDeuxConfirmationsSimultaneesDeLaMemeCommande() throws Exception {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var lecturesSansRecu = new CountDownLatch(2);
    doAnswer(invocation -> {
      var resultat = invocation.callRealMethod();
      lecturesSansRecu.countDown();
      assertThat(lecturesSansRecu.await(10, TimeUnit.SECONDS)).as("Les deux requetes ont lu l absence du recu").isTrue();
      return resultat;
    })
      .when(recus)
      .get(any());
    var action = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD))
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
    assertThat(relu.journal().evenement(proposition.adresse().pointage()).orElseThrow().annulation()).isPresent();
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isPresent();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNEnregistrerQuUneDesDeuxCommandesPrepareesSurLaMemeRevision() throws Exception {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var premiere = propositionDAnnulationDeTransition(suivi);
    var seconde = propositionDAnnulationDeTransition(suivi);
    var lecturesSansRecu = new CountDownLatch(2);
    doAnswer(invocation -> {
      var resultat = invocation.callRealMethod();
      lecturesSansRecu.countDown();
      assertThat(lecturesSansRecu.await(10, TimeUnit.SECONDS)).as("Les deux commandes ont lu l absence de leur recu").isTrue();
      return resultat;
    })
      .when(recus)
      .get(any());
    var premierActe = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(suivi.id(), premiere, CONTEXTE_LEROY_IMPECCMOLD))
    );
    var secondActe = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(suivi.id(), seconde, CONTEXTE_LEROY_IMPECCMOLD))
    );
    // WHEN THEN
    try (var executor = Executors.newFixedThreadPool(2)) {
      var premier = executor.submit(premierActe::get);
      var second = executor.submit(secondActe::get);
      var issues = java.util.Arrays.asList(premier.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
      assertThat(issues).filteredOn(java.util.Objects::isNull).hasSize(1);
      assertThat(issues).filteredOn(java.util.Objects::nonNull).singleElement().isExactlyInstanceOf(ApercuObsoleteException.class);
    }
    var relu = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
    assertThat(relu.revision().value()).isEqualTo(1);
    assertThat(relu.journal().evenements()).hasSize(2);
    var premierRecu = inTransaction(() -> recus.get(premiere.commande()));
    var secondRecu = inTransaction(() -> recus.get(seconde.commande()));
    assertThat(java.util.stream.Stream.of(premierRecu, secondRecu).filter(Optional::isPresent)).hasSize(1);
  }

  @ParameterizedTest
  @EnumSource(EcritureConcurrente.class)
  @WithTenant("impeccmold")
  void shouldRefuserLApercuQuandUneEcriturePubliqueGagnePendantLaConfirmation(EcritureConcurrente ecriture) throws Exception {
    // GIVEN
    var initial = suiviAvecTransitionDeMemeCategorie();
    var avant = ecriture == EcritureConcurrente.REOUVERTURE ? initial.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H)) : initial;
    var suivi = inTransaction(() -> suivis.create(avant));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var confirmationEnCours = new CountDownLatch(1);
    var ecritureConcurrenteTerminee = new CountDownLatch(1);
    doAnswer(invocation -> {
      var resultat = invocation.callRealMethod();
      if (confirmationEnCours.getCount() > 0) {
        confirmationEnCours.countDown();
        assertThat(ecritureConcurrenteTerminee.await(10, TimeUnit.SECONDS)).isTrue();
      }
      return resultat;
    })
      .when(recus)
      .get(any());
    var action = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD))
    );
    try (var executor = Executors.newSingleThreadExecutor()) {
      var confirmation = executor.submit(action::get);
      com.glm.glmback.atelier.domain.LectureDuSuivi gagnant;
      try {
        assertThat(confirmationEnCours.await(10, TimeUnit.SECONDS)).isTrue();
        // WHEN
        gagnant = switch (ecriture) {
          case POINTAGE -> atelier
            .pointeDuPupitre(
              PointageAEnregistrer.pupitreBuilder()
                .suivi(suivi.id())
                .type(TypeDEvenementDAtelier.FIN)
                .intention(IntentionDePointage.FIN)
                .activiteVisee(suivi.journal().evenements().getFirst().activite())
                .operateur(OPERATEUR_ID_DUPONT)
                .poste(Optional.empty())
                .auteur(AUTEUR_MARTIN)
                .dateDeSurvenue(Optional.of(LE_10_MAI_2026_A_17H))
                .evenement(com.glm.glmback.atelier.domain.EvenementDAtelierId.newId())
            )
            .agregat();
          case ANNULATION -> atelier.annule(((ActeDeResolution.Annulation) proposition.acte()).commande());
          case CLOTURE -> atelier.cloture(new ClotureAEnregistrer(suivi.id(), AUTEUR_MARTIN, Optional.of(LE_10_MAI_2026_A_17H)));
          case REOUVERTURE -> atelier.annuleLaCloture(suivi.id());
        };
      } finally {
        ecritureConcurrenteTerminee.countDown();
      }
      // THEN
      assertThat(confirmation.get(15, TimeUnit.SECONDS)).isExactlyInstanceOf(ApercuObsoleteException.class);
      assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(gagnant.suivi());
      assertThat(gagnant.suivi().revision().value()).isEqualTo(1);
      var identitesInitiales = suivi.journal().evenements().stream().map(com.glm.glmback.atelier.domain.EvenementDAtelier::id).toList();
      var identitesCourantes = gagnant
        .suivi()
        .journal()
        .evenements()
        .stream()
        .map(com.glm.glmback.atelier.domain.EvenementDAtelier::id)
        .toList();
      if (ecriture == EcritureConcurrente.POINTAGE) {
        assertThat(identitesCourantes).containsAll(identitesInitiales).hasSize(3);
      } else {
        assertThat(identitesCourantes).containsExactlyElementsOf(identitesInitiales);
      }
    }
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRetrouverLActeApresUneReponsePerdueEtRelireLeSuiviActuel() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    var avantCloture = inTransaction(() -> suivis.get(suivi.id())).orElseThrow();
    var cloture = inTransaction(() -> suivis.update(avantCloture.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))));
    // WHEN
    var resultat = confirmations.verifier(suivi.id(), proposition.commande(), CONTEXTE_LEROY_IMPECCMOLD);
    // THEN
    assertThat(resultat)
      .get()
      .satisfies(atteste -> {
        assertThat(atteste.recu().proposition()).isEqualTo(proposition);
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
    var proposition = propositionDAnnulationDeTransition(suivi);
    var preparationCommencee = new CountDownLatch(1);
    var termineLaPreparation = new CountDownLatch(1);
    doAnswer(invocation -> {
      var resultat = invocation.callRealMethod();
      if (preparationCommencee.getCount() > 0) {
        preparationCommencee.countDown();
        assertThat(termineLaPreparation.await(10, TimeUnit.SECONDS)).isTrue();
      }
      return resultat;
    })
      .when(recus)
      .get(any());
    var action = avecContexteDeRequete(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD));
    try (var executor = Executors.newSingleThreadExecutor()) {
      var premiereRequete = executor.submit(action::get);
      try {
        assertThat(preparationCommencee.await(10, TimeUnit.SECONDS)).isTrue();
        // WHEN THEN
        assertThat(confirmations.verifier(suivi.id(), proposition.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isEmpty();
      } finally {
        termineLaPreparation.countDown();
      }
      assertThat(premiereRequete.get(15, TimeUnit.SECONDS)).isNotNull();
    }
    assertThat(confirmations.verifier(suivi.id(), proposition.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isPresent();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldGarderLAbsenceDeRecuNonConcluanteApresInsertionAvantCommit() throws Exception {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var recuInsere = new CountDownLatch(1);
    var autoriseLeCommit = new CountDownLatch(1);
    var action = avecContexteDeRequete(() ->
      transactions.execute(status -> {
        var resultat = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
        assertThat(recus.get(proposition.commande())).contains(resultat.recu());
        recuInsere.countDown();
        try {
          assertThat(autoriseLeCommit.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interruption) {
          throw new AssertionError(interruption);
        }
        return resultat;
      })
    );
    // WHEN THEN
    try (var executor = Executors.newSingleThreadExecutor()) {
      var confirmation = executor.submit(action::get);
      try {
        assertThat(recuInsere.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(confirmations.verifier(suivi.id(), proposition.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isEmpty();
      } finally {
        autoriseLeCommit.countDown();
      }
      var resultat = confirmation.get(15, TimeUnit.SECONDS);
      assertThat(confirmations.verifier(suivi.id(), proposition.commande(), CONTEXTE_LEROY_IMPECCMOLD))
        .get()
        .satisfies(atteste -> assertThat(atteste.recu()).isEqualTo(resultat.recu()));
    }
  }

  @Test
  @WithTenant("impeccmold")
  void shouldReserverEtAssocierLIdentiteProspectiveDeLaRegularisation() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDeRegularisationDeFin(suivi);
    var evenement = proposition.evenement().orElseThrow();
    // WHEN
    var resultat = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    // THEN
    assertThat(
      inTransaction(() ->
        (
          (Number) entities
            .createNativeQuery("select count(*) from identite_evenement_atelier where id = ?")
            .setParameter(1, evenement.uuid())
            .getSingleResult()
        ).longValue()
      )
    ).isEqualTo(1);
    Object[] identite = inTransaction(() ->
      (Object[]) entities
        .createNativeQuery("select type_agregat, agregat_id from identite_evenement_atelier where id = ?")
        .setParameter(1, evenement.uuid())
        .getSingleResult()
    );
    assertThat(identite).containsExactly("SUIVI_D_ATELIER", suivi.id().uuid());
    assertThat(resultat.recu().evenementsTouches()).containsExactly(evenement);
    assertThat(resultat.dossier().lecture().suivi().journal().evenement(evenement))
      .get()
      .satisfies(fait ->
        assertThat(fait.horodatage().dateDeSurvenue()).isEqualTo(java.time.Instant.parse("2026-05-10T12:00:00.123456789Z"))
      );
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).contains(resultat.recu());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneCollisionDIdentiteSansRemplacementImplicite() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDeRegularisationDeFin(suivi);
    var evenement = proposition.evenement().orElseThrow();
    assertThat(inTransaction(() -> identites.reserveHorsPupitre(evenement.uuid()))).isTrue();
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      ApercuObsoleteException.class
    );
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejouerApresUnChangementDeNomSansRevaliderLeMetier() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    var premier = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    var courant = inTransaction(() -> suivis.update(premier.dossier().lecture().suivi().cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))));
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H.plusSeconds(3600));
    when(operateurs.get(any())).thenReturn(Optional.empty());
    clearInvocations(empreintes, operateurs);
    // WHEN
    var rejeu = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_RENOMME_IMPECCMOLD);
    // THEN
    assertThat(rejeu.recu()).isEqualTo(premier.recu());
    assertThat(rejeu.dossier().lecture().suivi()).isEqualTo(courant);
    verifyNoInteractions(empreintes, operateurs);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRecontrolerLeRoleSurLeRejeuEtLaVerification() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDAnnulationDeTransition(suivi);
    confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
    var authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext().setAuthentication(
      new JwtAuthenticationToken(authentication.getToken(), List.of(new SimpleGrantedAuthority("ROLE_USER")))
    );
    try {
      // WHEN THEN
      assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isInstanceOf(
        AccessDeniedException.class
      );
      assertThatThrownBy(() -> confirmations.verifier(suivi.id(), proposition.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isInstanceOf(
        AccessDeniedException.class
      );
    } finally {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnSuiviIntrouvableSansEcrireDeRecu() {
    // GIVEN
    var suivi = suiviAvecTransitionDeMemeCategorie();
    var proposition = propositionDAnnulationDeTransition(suivi);
    // WHEN THEN
    assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      SuiviDAtelierIntrouvableException.class
    );
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldCorrigerEnUnSeulActeAvecLAuteurActuelEtUnRecuExact() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDeCorrectionDeTransition(suivi);
    var remplacement = proposition.evenement().orElseThrow();
    var original = suivi.journal().evenement(proposition.adresse().pointage()).orElseThrow();
    // WHEN
    var resultat = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_RENOMME_IMPECCMOLD);
    // THEN
    var relu = resultat.dossier().lecture().suivi();
    assertThat(relu.revision().value()).isEqualTo(1);
    assertThat(relu.conflits()).isEmpty();
    assertThat(relu.journal().evenement(original.id()))
      .get()
      .satisfies(fait -> {
        assertThat(fait.annulation())
          .get()
          .satisfies(annulation -> {
            assertThat(annulation.auteur()).isEqualTo(AUTEUR_MARTIN);
            assertThat(annulation.date()).isEqualTo(LE_10_MAI_2026_A_17H);
          });
        assertThat(fait).isEqualTo(original.annule(fait.annulation().orElseThrow()));
      });
    assertThat(relu.journal().evenement(remplacement))
      .get()
      .satisfies(fait -> {
        assertThat(fait.remplace()).contains(original.id());
        assertThat(fait.auteur()).isEqualTo(AUTEUR_MARTIN);
        assertThat(fait.dateDeSurvenue()).isEqualTo(java.time.Instant.parse("2026-05-10T12:00:00.123456789Z"));
      });
    assertThat(resultat.recu().evenementsTouches()).containsExactly(original.id(), remplacement);
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).contains(resultat.recu());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldAnnulerEnsembleLesFaitsProjectionsReservationEtRecuSurRollback() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var proposition = propositionDeCorrectionDeTransition(suivi);
    var evenement = proposition.evenement().orElseThrow();
    var projectionsAvant = inTransaction(() ->
      entities
        .createNativeQuery("select * from activite_d_atelier where suivi_id = ? order by id")
        .setParameter(1, suivi.id().uuid())
        .getResultList()
    );
    // WHEN
    transactions.execute(status -> {
      var resultat = confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD);
      assertThat(resultat.recu().revisionEnregistree().value()).isEqualTo(1);
      assertThat(recus.get(proposition.commande())).contains(resultat.recu());
      status.setRollbackOnly();
      return resultat;
    });
    // THEN
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
    assertThat(
      inTransaction(() ->
        (
          (Number) entities
            .createNativeQuery("select count(*) from identite_evenement_atelier where id = ?")
            .setParameter(1, evenement.uuid())
            .getSingleResult()
        ).longValue()
      )
    ).isZero();
    var projectionsApres = inTransaction(() ->
      entities
        .createNativeQuery("select * from activite_d_atelier where suivi_id = ? order by id")
        .setParameter(1, suivi.id().uuid())
        .getResultList()
    );
    assertThat(projectionsApres).usingRecursiveComparison().isEqualTo(projectionsAvant);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldAnnulerToutesLesEcrituresDeLaCommandeReutiliseeSurUnAutreSuiviConcurrent() throws Exception {
    // GIVEN
    var premierSuivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var secondSuivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var premiere = propositionDeCorrectionDeTransition(premierSuivi);
    var seconde = ConcurrenceDesActesFixture.avecCommande(
      new ConcurrenceDesActesFixture.ChangementDeCommande(propositionDeCorrectionDeTransition(secondSuivi), premiere.commande())
    );
    var premiereProjection = inTransaction(() ->
      entities
        .createNativeQuery("select * from activite_d_atelier where suivi_id = ? order by id")
        .setParameter(1, premierSuivi.id().uuid())
        .getResultList()
    );
    var secondeProjection = inTransaction(() ->
      entities
        .createNativeQuery("select * from activite_d_atelier where suivi_id = ? order by id")
        .setParameter(1, secondSuivi.id().uuid())
        .getResultList()
    );
    var preparationsTerminees = new CountDownLatch(2);
    when(empreintes.calcule(any(), any())).thenAnswer(invocation -> {
      preparationsTerminees.countDown();
      assertThat(preparationsTerminees.await(10, TimeUnit.SECONDS))
        .as("Les deux transactions ont prepare leur acte apres la seconde lecture sans recu")
        .isTrue();
      return "consequences-annulation";
    });
    var premierActe = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(premierSuivi.id(), premiere, CONTEXTE_LEROY_IMPECCMOLD))
    );
    var secondActe = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(secondSuivi.id(), seconde, CONTEXTE_LEROY_IMPECCMOLD))
    );
    // WHEN THEN
    try (var executor = Executors.newFixedThreadPool(2)) {
      var premier = executor.submit(premierActe::get);
      var second = executor.submit(secondActe::get);
      var issues = java.util.Arrays.asList(premier.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
      assertThat(issues).filteredOn(java.util.Objects::isNull).hasSize(1);
      assertThat(issues).filteredOn(java.util.Objects::nonNull).singleElement().isExactlyInstanceOf(ConfirmationReutiliseeException.class);
    }
    var recu = inTransaction(() -> recus.get(premiere.commande())).orElseThrow();
    var premierGagne = recu.proposition().adresse().suivi().equals(premierSuivi.id());
    var perdant = premierGagne ? secondSuivi : premierSuivi;
    var propositionPerdante = premierGagne ? seconde : premiere;
    var projectionPerdante = premierGagne ? secondeProjection : premiereProjection;
    assertThat(inTransaction(() -> suivis.get(perdant.id()))).contains(perdant);
    assertThat(
      inTransaction(() ->
        entities
          .createNativeQuery("select * from activite_d_atelier where suivi_id = ? order by id")
          .setParameter(1, perdant.id().uuid())
          .getResultList()
      )
    )
      .usingRecursiveComparison()
      .isEqualTo(projectionPerdante);
    assertThat(
      inTransaction(() ->
        (
          (Number) entities
            .createNativeQuery("select count(*) from identite_evenement_atelier where id = ?")
            .setParameter(1, propositionPerdante.evenement().orElseThrow().uuid())
            .getSingleResult()
        ).longValue()
      )
    ).isZero();
    assertThat(
      inTransaction(() ->
        (
          (Number) entities
            .createNativeQuery("select count(*) from identite_evenement_atelier where id = ?")
            .setParameter(1, recu.proposition().evenement().orElseThrow().uuid())
            .getSingleResult()
        ).longValue()
      )
    ).isEqualTo(1);
    var gagnant = inTransaction(() -> suivis.get(recu.proposition().adresse().suivi())).orElseThrow();
    assertThat(gagnant.revision().value()).isEqualTo(1);
    assertThat(gagnant.journal().evenements()).hasSize(3);
    assertThat(recu.revisionEnregistree().value()).isEqualTo(1);
  }

  @ParameterizedTest
  @EnumSource(value = EtatDAdresseDossier.class, names = { "ANCRE_ANNULEE", "INTROUVABLE", "HORS_CONFLIT" })
  @WithTenant("impeccmold")
  void shouldRefuserUneAdresseObsoleteMemeSiLaRevisionEtLEmpreinteSontCourantes(EtatDAdresseDossier etat) {
    var conflit = suiviAvecTransitionDeMemeCategorie();
    var transition = conflit.journal().evenements().getLast();
    var suivi = inTransaction(() -> suivis.create(conflit.annule(transition.id(), annulationParLeroy())));
    var base = propositionDAnnulationDeTransition(suivi);
    var ancre = switch (etat) {
      case ANCRE_ANNULEE -> transition.id();
      case INTROUVABLE -> EvenementDAtelierId.newId();
      default -> suivi.journal().evenements().getFirst().id();
    };
    var proposition = PropositionAConfirmer.builder()
      .commande(base.commande())
      .adresse(new AdresseDossierConflit(suivi.id(), ancre))
      .revision(suivi.revision())
      .acte(base.acte())
      .evenement(base.evenement())
      .empreinteConsequences(base.empreinteConsequences());
    assertThatThrownBy(() -> confirmations.confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD)).isExactlyInstanceOf(
      ApercuObsoleteException.class
    );
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(proposition.commande()))).isEmpty();
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

  private enum EcritureConcurrente {
    POINTAGE,
    ANNULATION,
    CLOTURE,
    REOUVERTURE,
  }
}

package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.ConfirmerLesActes;
import com.glm.glmback.atelier.application.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.RecusDActes;
import com.glm.glmback.atelier.application.ReferencesDApercu;
import com.glm.glmback.atelier.domain.ApercuInvalideException;
import com.glm.glmback.atelier.domain.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.ConfirmationReutiliseeException;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;

@IntegrationTest
class ConfirmationsDActesIT {

  @Autowired
  private ConfirmerLesActes confirmations;

  @Autowired
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
  private ReferencesDApercu references;

  @MockitoBean
  private EmpreintesDesConsequences empreintes;

  @MockitoBean
  private Clock clock;

  @MockitoBean
  private OperateursConnus operateurs;

  @BeforeEach
  void evaluation() {
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    when(empreintes.calcule(any(), any())).thenReturn("consequences-annulation");
    when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
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
  void shouldNEnregistrerQuUneDesDeuxCommandesPrepareesSurLaMemeRevision() throws Exception {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var premiere = preuveDAnnulationDeTransition(suivi);
    var seconde = preuveDAnnulationDeTransition(suivi);
    var lecturesSansRecu = new CountDownLatch(2);
    when(references.read(anyString())).thenAnswer(invocation -> {
      lecturesSansRecu.countDown();
      assertThat(lecturesSansRecu.await(10, TimeUnit.SECONDS)).as("Les deux commandes ont lu l absence de leur recu").isTrue();
      return invocation.getArgument(0).equals("premiere-reference") ? premiere : seconde;
    });
    var premierActe = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(suivi.id(), premiere.commande(), "premiere-reference", CONTEXTE_LEROY_IMPECCMOLD))
    );
    var secondActe = avecContexteDeRequete(() ->
      catchThrowable(() -> confirmations.confirmer(suivi.id(), seconde.commande(), "seconde-reference", CONTEXTE_LEROY_IMPECCMOLD))
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

  @Test
  @WithTenant("impeccmold")
  void shouldReserverEtAssocierLIdentiteProspectiveDeLaRegularisation() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDeRegularisationDeFin(suivi);
    var evenement = preuve.evenement().orElseThrow();
    when(references.read("reference-regularisation")).thenReturn(preuve);
    // WHEN
    var resultat = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-regularisation", CONTEXTE_LEROY_IMPECCMOLD);
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
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).contains(resultat.recu());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneCollisionDIdentiteSansRemplacementImplicite() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDeRegularisationDeFin(suivi);
    var evenement = preuve.evenement().orElseThrow();
    when(references.read("reference-regularisation")).thenReturn(preuve);
    assertThat(inTransaction(() -> identites.reserveHorsPupitre(evenement.uuid()))).isTrue();
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-regularisation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejouerAvantLeCodecEtLExpirationSansRevaliderLeMetier() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    var premier = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    var courant = inTransaction(() -> suivis.update(premier.dossier().lecture().suivi().cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H))));
    when(clock.now()).thenReturn(preuve.expireLe().plusSeconds(1));
    when(references.read(anyString())).thenThrow(new ApercuInvalideException());
    when(operateurs.get(any())).thenReturn(Optional.empty());
    clearInvocations(references, empreintes, operateurs);
    // WHEN
    var rejeu = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_RENOMME_IMPECCMOLD);
    // THEN
    assertThat(rejeu.recu()).isEqualTo(premier.recu());
    assertThat(rejeu.dossier().lecture().suivi()).isEqualTo(courant);
    verifyNoInteractions(references, empreintes, operateurs);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRecontrolerLeRoleSurLeRejeuEtLaVerification() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD);
    var authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext().setAuthentication(
      new JwtAuthenticationToken(authentication.getToken(), List.of(new SimpleGrantedAuthority("ROLE_USER")))
    );
    try {
      // WHEN THEN
      assertThatThrownBy(() ->
        confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
      ).isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> confirmations.verifier(suivi.id(), preuve.commande(), CONTEXTE_LEROY_IMPECCMOLD)).isInstanceOf(
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
    var preuve = preuveDAnnulationDeTransition(suivi);
    when(references.read("reference-annulation")).thenReturn(preuve);
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), preuve.commande(), "reference-annulation", CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldCorrigerEnUnSeulActeAvecLAuteurActuelEtUnRecuExact() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDeCorrectionDeTransition(suivi);
    var remplacement = preuve.evenement().orElseThrow();
    var original = suivi.journal().evenement(preuve.adresse().pointage()).orElseThrow();
    when(references.read("reference-correction")).thenReturn(preuve);
    // WHEN
    var resultat = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-correction", CONTEXTE_LEROY_RENOMME_IMPECCMOLD);
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
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).contains(resultat.recu());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldAnnulerEnsembleLesFaitsProjectionsReservationEtRecuSurRollback() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var preuve = preuveDeCorrectionDeTransition(suivi);
    var evenement = preuve.evenement().orElseThrow();
    when(references.read("reference-correction")).thenReturn(preuve);
    var projectionsAvant = inTransaction(() ->
      entities
        .createNativeQuery("select * from activite_d_atelier where suivi_id = ? order by id")
        .setParameter(1, suivi.id().uuid())
        .getResultList()
    );
    // WHEN
    transactions.execute(status -> {
      var resultat = confirmations.confirmer(suivi.id(), preuve.commande(), "reference-correction", CONTEXTE_LEROY_IMPECCMOLD);
      assertThat(resultat.recu().revisionEnregistree().value()).isEqualTo(1);
      assertThat(recus.get(preuve.commande())).contains(resultat.recu());
      status.setRollbackOnly();
      return resultat;
    });
    // THEN
    assertThat(inTransaction(() -> suivis.get(suivi.id()))).contains(suivi);
    assertThat(inTransaction(() -> recus.get(preuve.commande()))).isEmpty();
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
}

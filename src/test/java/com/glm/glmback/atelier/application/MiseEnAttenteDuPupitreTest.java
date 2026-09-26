package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.GesteDAtelier;
import com.glm.glmback.atelier.domain.GesteDePresence;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PointagesEnAttenteEnMemoire;
import com.glm.glmback.atelier.domain.PointagesSignales;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierClotureException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lot 8c de la strategie « bornes de fin de journee » : ce que le pupitre recoit d'un geste qu'on ne sait rattacher a
 * rien. Un succes sans agregat, et un pointage en attente pour le gestionnaire ; son identifiant y est associe, pour
 * qu'un rejeu rende exactement la meme chose.
 */
@UnitTest
class MiseEnAttenteDuPupitreTest {

  private static final OperateurId OPERATEUR_INCONNU = new OperateurId(UUID.fromString("5e3d1c08-7f42-4a96-b0e5-2c8d9a1b3f74"));

  private final JourneeDeTravailRepository journees = Mockito.mock(JourneeDeTravailRepository.class);
  private final SuiviDAtelierRepository suivis = Mockito.mock(SuiviDAtelierRepository.class);
  private final OperateursConnus operateurs = Mockito.mock(OperateursConnus.class);
  private final IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
  private final PointagesEnAttenteEnMemoire enAttente = new PointagesEnAttenteEnMemoire();

  @Test
  void shouldMettreEnAttenteLArriveeDUnOperateurInconnu() {
    // GIVEN
    EvenementDePresenceId geste = EvenementDePresenceId.newId();
    given(identites.reserve(any(), any())).willReturn(ReservationDEvenement.inedite());

    // WHEN
    ResultatDEcriture<JourneeDeTravail> resultat = presence().arriveDuPupitre(
      new ArriveeAEnregistrer(OPERATEUR_INCONNU, AUTEUR_DUPONT, Optional.of(LE_10_MAI_2026_A_7H), geste)
    );

    // THEN
    PointageEnAttente pointage = seulEnAttente();
    assertThat(resultat).isEqualTo(ResultatDEcriture.enAttente(false));
    assertThat(pointage.evenementDuPupitre()).isEqualTo(geste.uuid());
    assertThat(pointage.motif()).isEqualTo(MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    assertThat(pointage.geste()).isEqualTo(
      new GesteDePresence(OPERATEUR_INCONNU, TypeDEvenementDePresence.ARRIVEE, Optional.of(LE_10_MAI_2026_A_7H))
    );
    assertThat(pointage.auteur()).isEqualTo(AUTEUR_DUPONT);
    assertThat(pointage.dateDeReception()).isEqualTo(LE_10_MAI_2026_A_8H);
    then(identites).should().associe(geste.uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.POINTAGE_EN_ATTENTE, pointage.id().uuid()));
    then(journees).should(never()).create(any());
  }

  @Test
  void shouldMettreEnAttenteUnePauseDUnOperateurInconnu() {
    // GIVEN
    given(identites.reserve(any(), any())).willReturn(ReservationDEvenement.inedite());

    // WHEN
    ResultatDEcriture<JourneeDeTravail> resultat = presence().pointeDuPupitre(pause(OPERATEUR_INCONNU));

    // THEN
    assertThat(resultat).isEqualTo(ResultatDEcriture.enAttente(false));
    assertThat(seulEnAttente().motif()).isEqualTo(MotifDeMiseEnAttente.OPERATEUR_INCONNU);
  }

  /**
   * Rejouee dans le desordre, la pause tombe dans une journee deja fermee : elle ne se rattache a aucun enchainement.
   */
  @Test
  void shouldMettreEnAttenteUnGesteDateDansUneJourneeFermee() {
    // GIVEN
    given(identites.reserve(any(), any())).willReturn(ReservationDEvenement.inedite());
    given(journees.getEnCoursPour(OPERATEUR_ID_DUPONT)).willReturn(Optional.empty());
    given(journees.journeesDeLOperateurSur(eq(OPERATEUR_ID_DUPONT), any())).willReturn(List.of(journeeDeDupontDe7HA17HAvecPauseDeMidi()));

    // WHEN
    ResultatDEcriture<JourneeDeTravail> resultat = presence().pointeDuPupitre(pause(OPERATEUR_ID_DUPONT));

    // THEN
    assertThat(resultat).isEqualTo(ResultatDEcriture.enAttente(false));
    assertThat(seulEnAttente().motif()).isEqualTo(MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE);
    then(journees).should(never()).update(any());
  }

  /**
   * L'identifiant appartient deja a un autre contenu : le geste est mis en attente sans lui etre associe.
   */
  @Test
  void shouldMettreEnAttenteUnIdentifiantReutiliseSansLAssocier() {
    // GIVEN
    PointageDePresenceAEnregistrer pause = pause(OPERATEUR_ID_DUPONT);
    given(identites.reserve(any(), any())).willThrow(new IdentifiantDEvenementReutiliseException(pause.evenement().uuid()));

    // WHEN
    ResultatDEcriture<JourneeDeTravail> resultat = presence().pointeDuPupitre(pause);

    // THEN
    assertThat(resultat).isEqualTo(ResultatDEcriture.enAttente(false));
    assertThat(seulEnAttente().motif()).isEqualTo(MotifDeMiseEnAttente.IDENTIFIANT_REUTILISE);
    then(identites).should(never()).associe(any(), any());
  }

  @Test
  void shouldRejouerUnGesteDejaMisEnAttente() {
    // GIVEN
    given(identites.reserve(any(), any())).willReturn(
      ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.POINTAGE_EN_ATTENTE, UUID.randomUUID()))
    );

    // WHEN
    ResultatDEcriture<JourneeDeTravail> arrivee = presence().arriveDuPupitre(
      new ArriveeAEnregistrer(OPERATEUR_INCONNU, AUTEUR_DUPONT, Optional.empty(), EvenementDePresenceId.newId())
    );
    ResultatDEcriture<JourneeDeTravail> pause = presence().pointeDuPupitre(pause(OPERATEUR_INCONNU));
    ResultatDEcriture<SuiviDAtelier> debut = atelier().pointeDuPupitre(debut(SuiviDAtelierId.newId()));

    // THEN
    assertThat(List.of(arrivee, pause, debut)).containsOnly(ResultatDEcriture.enAttente(true));
    assertThat(enAttente.tous()).isEmpty();
    then(journees).shouldHaveNoInteractions();
    then(suivis).shouldHaveNoInteractions();
  }

  @Test
  void shouldMettreEnAttenteUnPointageSurUnElementInconnu() {
    // GIVEN
    SuiviDAtelierId inconnu = SuiviDAtelierId.newId();
    PointageAEnregistrer debut = debut(inconnu);
    given(identites.reserve(any(), any())).willReturn(ReservationDEvenement.inedite());
    given(suivis.get(inconnu)).willReturn(Optional.empty());

    // WHEN
    ResultatDEcriture<SuiviDAtelier> resultat = atelier().pointeDuPupitre(debut);

    // THEN
    PointageEnAttente pointage = seulEnAttente();
    assertThat(resultat).isEqualTo(ResultatDEcriture.enAttente(false));
    assertThat(pointage.motif()).isEqualTo(MotifDeMiseEnAttente.ELEMENT_INCONNU);
    assertThat(pointage.geste()).isEqualTo(
      GesteDAtelier.builder()
        .suivi(inconnu)
        .operateur(OPERATEUR_ID_DUPONT)
        .type(TypeDEvenementDAtelier.DEBUT)
        .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
        .dateDeclaree(Optional.of(LE_10_MAI_2026_A_7H30))
    );
    then(identites)
      .should()
      .associe(debut.evenement().uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.POINTAGE_EN_ATTENTE, pointage.id().uuid()));
  }

  /**
   * L'element cloture reste le seul refus montre a l'operateur : rien n'est mis en attente.
   */
  @Test
  void shouldToujoursRefuserUnDebutSurUnElementCloture() {
    // GIVEN
    SuiviDAtelier cloture = suiviDAtelierEngage().cloture(clotureParLeroyA(LE_10_MAI_2026_A_7H));
    given(identites.reserve(any(), any())).willReturn(ReservationDEvenement.inedite());
    given(suivis.get(cloture.id())).willReturn(Optional.of(cloture));
    SuivisDAtelierApplicationService service = atelier();
    PointageAEnregistrer debut = debut(cloture.id());

    // WHEN / THEN
    assertThatThrownBy(() -> service.pointeDuPupitre(debut)).isExactlyInstanceOf(SuiviDAtelierClotureException.class);
    assertThat(enAttente.tous()).isEmpty();
  }

  /**
   * Appliquee par le gestionnaire, l'arrivee prend une identite serveur, associee a la journee ouverte.
   */
  @Test
  void shouldAppliquerUneArriveeSousUneIdentiteServeur() {
    // GIVEN
    given(identites.reserveHorsPupitre(any())).willReturn(true);
    given(journees.getEnCoursPour(OPERATEUR_ID_DUPONT)).willReturn(Optional.empty());
    given(journees.create(any())).willAnswer(invocation -> invocation.getArgument(0));

    // WHEN
    JourneeDeTravail ouverte = presence().applique(
      new GesteDePresence(OPERATEUR_ID_DUPONT, TypeDEvenementDePresence.ARRIVEE, Optional.empty()),
      LE_10_MAI_2026_A_7H,
      AUTEUR_LEROY
    );

    // THEN
    UUID arrivee = ouverte.journal().evenements().getFirst().id().uuid();
    then(identites).should().reserveHorsPupitre(arrivee);
    then(identites).should().associe(arrivee, new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, ouverte.id().uuid()));
    assertThat(ouverte.journal().evenements().getFirst().auteur()).isEqualTo(AUTEUR_LEROY);
  }

  private PointageEnAttente seulEnAttente() {
    assertThat(enAttente.tous()).hasSize(1);
    return enAttente.tous().getFirst();
  }

  private static PointageDePresenceAEnregistrer pause(OperateurId operateur) {
    return new PointageDePresenceAEnregistrer(
      operateur,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.PAUSE,
      Optional.of(LE_10_MAI_2026_A_7H30),
      EvenementDePresenceId.newId()
    );
  }

  private static PointageAEnregistrer debut(SuiviDAtelierId suivi) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT)
      .dateDeSurvenue(Optional.of(LE_10_MAI_2026_A_7H30))
      .evenement(new EvenementDAtelierId(UUID.randomUUID()));
  }

  private JourneesDeTravailApplicationService presence() {
    given(operateurs.existe(OPERATEUR_ID_DUPONT)).willReturn(true);

    return new JourneesDeTravailApplicationService(
      journees,
      operateurs,
      Mockito.mock(PostesConnus.class),
      () -> AMPLITUDE_MAXIMALE_13H,
      Mockito.mock(PointagesSignales.class),
      enAttente,
      () -> LE_10_MAI_2026_A_8H,
      identites,
      new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class))
    );
  }

  private SuivisDAtelierApplicationService atelier() {
    return new SuivisDAtelierApplicationService(
      suivis,
      journees,
      Mockito.mock(ElementsEngageables.class),
      operateurs,
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Habilitations.class),
      () -> AMPLITUDE_MAXIMALE_13H,
      Mockito.mock(PointagesSignales.class),
      enAttente,
      () -> LE_10_MAI_2026_A_8H,
      identites,
      new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class))
    );
  }
}

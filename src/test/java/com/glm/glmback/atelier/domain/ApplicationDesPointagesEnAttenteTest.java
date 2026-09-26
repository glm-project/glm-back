package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Lot 8c de la strategie « bornes de fin de journee » : le gestionnaire applique un geste mis en attente. C'est une
 * regularisation, datee du geste et enregistree a l'instant de l'application, refusable avec explication.
 */
@UnitTest
class ApplicationDesPointagesEnAttenteTest {

  private static final EvenementDePresenceId REGULARISATION = EvenementDePresenceId.newId();

  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final JourneesDeTravailService service = JourneesDeTravailService.builder()
    .repository(journees)
    .operateurs(RessourcesDAtelierEnMemoire.deLAtelier().operateurs())
    .seuil(() -> AMPLITUDE_MAXIMALE_13H)
    .signalements(new PointagesSignalesEnMemoire())
    .clock(() -> LE_11_MAI_2026_A_9H15);

  @Test
  void shouldOuvrirUneJourneeEnAppliquantUneArrivee() {
    JourneeDeTravail ouverte = service.applique(geste(TypeDEvenementDePresence.ARRIVEE), LE_10_MAI_2026_A_7H, AUTEUR_LEROY, REGULARISATION);

    assertThat(ouverte.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(ouverte.journal().evenements())
      .extracting(
        EvenementDePresence::id,
        EvenementDePresence::type,
        EvenementDePresence::auteur,
        EvenementDePresence::dateDeSurvenue,
        EvenementDePresence::dateDEnregistrement
      )
      .containsExactly(tuple(REGULARISATION, TypeDEvenementDePresence.ARRIVEE, AUTEUR_LEROY, LE_10_MAI_2026_A_7H, LE_11_MAI_2026_A_9H15));
    assertThat(journees.get(ouverte.id())).contains(ouverte);
  }

  /**
   * Le depart du soir, rejoue dans le desordre par un pupitre, s'inscrit dans la journee qui contient sa date.
   */
  @Test
  void shouldInscrireLeGesteDansLaJourneeQuiContientSaDate() {
    JourneeDeTravail lundi = journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    Instant avantLeDepart = Instant.parse("2026-05-10T15:00:00Z");

    JourneeDeTravail regularisee = service.applique(geste(TypeDEvenementDePresence.PAUSE), avantLeDepart, AUTEUR_LEROY, REGULARISATION);

    assertThat(regularisee.id()).isEqualTo(lundi.id());
    assertThat(regularisee.journal().evenements())
      .filteredOn(evenement -> evenement.id().equals(REGULARISATION))
      .extracting(EvenementDePresence::type, EvenementDePresence::dateDeSurvenue)
      .containsExactly(tuple(TypeDEvenementDePresence.PAUSE, avantLeDepart));
  }

  @Test
  void shouldInscrireLeGesteDansLaJourneeEnCoursADefaut() {
    JourneeDeTravail enCours = journees.create(journeeDeDupontOuverteA7H());

    JourneeDeTravail regularisee = service.applique(
      geste(TypeDEvenementDePresence.DEPART),
      LE_10_MAI_2026_A_17H,
      AUTEUR_LEROY,
      REGULARISATION
    );

    assertThat(regularisee.id()).isEqualTo(enCours.id());
    assertThat(regularisee.etat()).isEqualTo(EtatDePresence.ABSENT);
  }

  @Test
  void shouldRefuserUnGesteSansJourneeOuLInscrire() {
    GesteDePresence depart = geste(TypeDEvenementDePresence.DEPART);

    assertThatThrownBy(() -> service.applique(depart, LE_10_MAI_2026_A_17H, AUTEUR_LEROY, REGULARISATION)).isExactlyInstanceOf(
      AucuneJourneeDeTravailEnCoursException.class
    );
  }

  @Test
  void shouldRefuserUnOperateurToujoursInconnu() {
    OperateurId inconnu = new OperateurId(UUID.randomUUID());

    assertThatThrownBy(() ->
      service.applique(
        new GesteDePresence(inconnu, TypeDEvenementDePresence.ARRIVEE, Optional.empty()),
        LE_10_MAI_2026_A_7H,
        AUTEUR_LEROY,
        REGULARISATION
      )
    ).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
    assertThatThrownBy(() ->
      service.applique(
        new GesteDePresence(inconnu, TypeDEvenementDePresence.PAUSE, Optional.empty()),
        LE_10_MAI_2026_A_12H,
        AUTEUR_LEROY,
        REGULARISATION
      )
    ).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
  }

  @Test
  void shouldRefuserAuGestionnaireUnGesteQuiCasseLEnchainement() {
    journees.create(journeeDeDupontOuverteA7H());
    GesteDePresence reprise = geste(TypeDEvenementDePresence.REPRISE);

    assertThatThrownBy(() -> service.applique(reprise, LE_10_MAI_2026_A_9H, AUTEUR_LEROY, REGULARISATION)).isExactlyInstanceOf(
      TransitionDePresenceInterditeException.class
    );
  }

  @Test
  void shouldAppliquerUnGesteDAtelierCommeUneRegularisation() {
    SuiviDAtelierId suivi = SuiviDAtelierId.newId();
    GesteDAtelier geste = GesteDAtelier.builder()
      .suivi(suivi)
      .operateur(OPERATEUR_ID_MARTIN)
      .type(TypeDEvenementDAtelier.DEBUT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .dateDeclaree(Optional.of(LE_10_MAI_2026_A_8H));

    RegularisationAEnregistrer regularisation = geste.regularisation(AUTEUR_LEROY, LE_10_MAI_2026_A_8H);

    assertThat(regularisation).isEqualTo(
      RegularisationAEnregistrer.builder()
        .suivi(suivi)
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_MARTIN)
        .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_8H)
    );
  }

  private static GesteDePresence geste(TypeDEvenementDePresence type) {
    return new GesteDePresence(OPERATEUR_ID_DUPONT, type, Optional.empty());
  }
}

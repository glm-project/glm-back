package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneesDeTravailServiceTest {

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final JourneesDeTravailService service = new JourneesDeTravailService(journees, ressources.operateurs(), maintenant::get);

  @Test
  void shouldOuvrirLaJourneeALArrivee() {
    JourneeDeTravail journee = service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));

    assertThat(journee.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(journee.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(journee.estEnCours()).isTrue();
    assertThat(service.get(journee.id())).isEqualTo(journee);
  }

  @Test
  void shouldOuvrirLaJourneeAUneHeureRegularisee() {
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail journee = service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_7H)));

    assertThat(journee.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(journee.journal().evenements().getFirst().dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
  }

  @Test
  void shouldNotOuvrirDeuxJourneesEnMemeTemps() {
    service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
    ArriveeAEnregistrer seconde = new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT);

    assertThatThrownBy(() -> service.arrive(seconde))
      .isExactlyInstanceOf(JourneeDeTravailDejaOuverteException.class)
      .hasMessageContaining(OPERATEUR_ID_DUPONT.uuid().toString());
  }

  /**
   * Le bouton de pause unique du client : un seul evenement, quel que soit le nombre d'elements en cours.
   */
  @Test
  void shouldPointerPauseRepriseEtDepartSurLaJourneeEnCours() {
    service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));

    maintenant.set(LE_10_MAI_2026_A_12H);
    service.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.PAUSE));
    maintenant.set(LE_10_MAI_2026_A_13H);
    service.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.REPRISE));
    maintenant.set(LE_10_MAI_2026_A_17H);
    JourneeDeTravail journee = service.pointe(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.DEPART)
    );

    assertThat(journee.estEnCours()).isFalse();
    assertThat(journee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
    assertThat(journee.fenetres()).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H)),
      new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
    );
  }

  /**
   * La presence est le seul acte ou l'operateur est verifie : aucun poste n'y intervient, donc aucune habilitation.
   */
  @Test
  void shouldNotOuvrirUneJourneePourUnOperateurInconnu() {
    ArriveeAEnregistrer inconnu = new ArriveeAEnregistrer(new OperateurId(UUID.randomUUID()), AUTEUR_LEROY);

    assertThatThrownBy(() -> service.arrive(inconnu)).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
  }

  @Test
  void shouldNotPointerSansJourneeEnCours() {
    PointageDePresenceAEnregistrer pause = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.PAUSE
    );

    assertThatThrownBy(() -> service.pointe(pause))
      .isExactlyInstanceOf(AucuneJourneeDeTravailEnCoursException.class)
      .hasMessageContaining(OPERATEUR_ID_DUPONT.uuid().toString());
  }

  @Test
  void shouldRegulariserUnDepartOublie() {
    JourneeDeTravail ouverte = service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail journee = service.regularise(
      RegularisationDePresenceAEnregistrer.builder()
        .journee(ouverte.id())
        .type(TypeDEvenementDePresence.DEPART)
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_17H)
    );

    assertThat(journee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
    assertThat(journee.journal().evenements().getLast().horodatage().estDifferee()).isTrue();
  }

  @Test
  void shouldNotRegulariserUneJourneeInconnue() {
    RegularisationDePresenceAEnregistrer inconnue = RegularisationDePresenceAEnregistrer.builder()
      .journee(JourneeDeTravailId.newId())
      .type(TypeDEvenementDePresence.DEPART)
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(LE_10_MAI_2026_A_17H);

    assertThatThrownBy(() -> service.regularise(inconnue)).isExactlyInstanceOf(JourneeDeTravailIntrouvableException.class);
  }

  @Test
  void shouldAnnulerUnPointageEnTrop() {
    JourneeDeTravail ouverte = service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
    maintenant.set(LE_10_MAI_2026_A_12H);
    JourneeDeTravail avecPause = service.pointe(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.PAUSE)
    );
    EvenementDePresenceId pause = avecPause.journal().evenements().getLast().id();

    JourneeDeTravail journee = service.annule(
      AnnulationDePresenceAEnregistrer.builder().journee(ouverte.id()).evenement(pause).auteur(AUTEUR_LEROY).motif(MOTIF_ERREUR_DE_SAISIE)
    );

    assertThat(journee.etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(journee.journal().evenements()).hasSize(2);
  }

  @Test
  void shouldCorrigerUneHeureDArriveeFausse() {
    maintenant.set(LE_10_MAI_2026_A_9H);
    JourneeDeTravail ouverte = service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
    EvenementDePresenceId arrivee = ouverte.journal().evenements().getFirst().id();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail journee = service.corrige(
      new CorrectionDePresenceAEnregistrer(
        arrivee,
        MOTIF_ERREUR_DE_SAISIE,
        RegularisationDePresenceAEnregistrer.builder()
          .journee(ouverte.id())
          .type(TypeDEvenementDePresence.ARRIVEE)
          .auteur(AUTEUR_LEROY)
          .dateDeSurvenue(LE_10_MAI_2026_A_7H)
      )
    );

    assertThat(journee.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(journee.journal().evenements()).hasSize(2);
  }

  @Test
  void shouldNotGetJourneeInconnue() {
    JourneeDeTravailId inconnue = JourneeDeTravailId.newId();

    assertThatThrownBy(() -> service.get(inconnue)).isExactlyInstanceOf(JourneeDeTravailIntrouvableException.class);
  }

  @Test
  void shouldListerLesJourneesDUnOperateurSurUnePeriode() {
    JourneeDeTravail journee = service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));

    assertThat(
      service.list(Optional.of(journeeDu10Mai2026()), Optional.of(OPERATEUR_ID_DUPONT), firstPageOfTen()).content()
    ).containsExactly(journee);
  }
}

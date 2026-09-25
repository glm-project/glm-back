package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeDeTravailTest {

  private static final JourneeDeTravailId ID = JourneeDeTravailId.newId();

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new JourneeDeTravail(null, OPERATEUR_ID_DUPONT, JournalDePresence.vide()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new JourneeDeTravail(ID, null, JournalDePresence.vide()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutJournal() {
    assertThatThrownBy(() -> new JourneeDeTravail(ID, OPERATEUR_ID_DUPONT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldOuvrirUneJourneeVide() {
    JourneeDeTravail journee = JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT);

    assertThat(journee.id()).isEqualTo(ID);
    assertThat(journee.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(journee.etat()).isEqualTo(EtatDePresence.ABSENT);
    assertThat(journee.estEnCours()).isFalse();
    assertThat(journee.debut()).isEmpty();
    assertThat(journee.contient(LE_10_MAI_2026_A_8H)).isFalse();
  }

  @Test
  void shouldEtreEnCoursDesLArrivee() {
    JourneeDeTravail journee = journeeDeDupontOuverteA7H();

    assertThat(journee.estEnCours()).isTrue();
    assertThat(journee.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(journee.fenetres()).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.empty()));
    assertThat(journee.amplitude()).isEmpty();
  }

  @Test
  void shouldSeFermerAuDepart() {
    JourneeDeTravail journee = journeeDeDupontDe7HA17HAvecPauseDeMidi();

    assertThat(journee.estEnCours()).isFalse();
    assertThat(journee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
    assertThat(journee.fenetres()).hasSize(2);
  }

  @Test
  void shouldContenirLesInstantsDeLaJourneeFermee() {
    JourneeDeTravail journee = journeeDeDupontDe7HA17HAvecPauseDeMidi();

    assertThat(journee.contient(LE_10_MAI_2026_A_7H)).isTrue();
    assertThat(journee.contient(LE_10_MAI_2026_A_12H)).isTrue();
    assertThat(journee.contient(LE_10_MAI_2026_A_17H)).isTrue();
    assertThat(journee.contient(LE_10_MAI_2026_A_7H.minusSeconds(1))).isFalse();
    assertThat(journee.contient(LE_11_MAI_2026_A_9H15)).isFalse();
  }

  @Test
  void shouldContenirTousLesInstantsPosterieursDUneJourneeOuverte() {
    assertThat(journeeDeDupontOuverteA7H().contient(LE_11_MAI_2026_A_9H15)).isTrue();
  }

  @Test
  void shouldAnnulerUnEvenement() {
    JourneeDeTravail journee = journeeDeDupontDe7HA17HAvecPauseDeMidi();
    EvenementDePresenceId depart = journee.journal().evenements().getLast().id();

    JourneeDeTravail sansDepart = journee.annule(depart, annulationParLeroy());

    assertThat(sansDepart.estEnCours()).isTrue();
    assertThat(sansDepart.id()).isEqualTo(journee.id());
  }

  @Test
  void shouldCorrigerUnEvenement() {
    JourneeDeTravail journee = JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT).enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_9H));
    EvenementDePresenceId arrivee = journee.journal().evenements().getFirst().id();

    JourneeDeTravail corrigee = journee.corrige(arrivee, annulationParLeroy(), arriveeDeDupontA(LE_10_MAI_2026_A_7H));

    assertThat(corrigee.debut()).contains(LE_10_MAI_2026_A_7H);
  }

  @Test
  void shouldNeJamaisEtreAbandonneeSansArrivee() {
    JourneeDeTravail vide = JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT);

    assertThat(vide.estAbandonneePour(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNePasEtreAbandonneeSousLeSeuil() {
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(LE_10_MAI_2026_A_17H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  /**
   * D2 : abandonnee quand l'amplitude depasse le seuil, pas quand elle l'atteint. Un geste a 20:00 pile reste dans la
   * journee ouverte a 07:00.
   */
  @Test
  void shouldNePasEtreAbandonneeAuSeuilPile() {
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(LE_10_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldEtreAbandonneeUneSecondeApresLeSeuil() {
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(LE_10_MAI_2026_A_20H.plusSeconds(1), AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  @Test
  void shouldMesurerLeSeuilPausesComprises() {
    JourneeDeTravail enPause = journeeDeDupontOuverteA7H().enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H));

    assertThat(enPause.estAbandonneePour(LE_11_MAI_2026_A_7H, AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  @Test
  void shouldAppliquerLeSeuilDonne() {
    Instant a17h30 = LE_10_MAI_2026_A_17H.plusSeconds(1800);

    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(a17h30, AMPLITUDE_MAXIMALE_10H)).isTrue();
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(a17h30, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNeJamaisAbandonnerUneJourneeFermeeParUnDepart() {
    assertThat(journeeDeDupontDe7HA17HAvecPauseDeMidi().estAbandonneePour(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNAvoirAucuneEtendueSansEvenement() {
    assertThat(JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT).etendue()).isEmpty();
  }

  @Test
  void shouldEtendreUneJourneeOuverteJusquASonDernierFaitConnu() {
    JourneeDeTravail journee = journeeDeDupontOuverteA7H().enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H));

    assertThat(journee.etendue()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_12H));
  }

  @Test
  void shouldEtendreUneJourneeFermeeDeLArriveeAuDepart() {
    assertThat(journeeDeDupontDe7HA17HAvecPauseDeMidi().etendue()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldEcarterDeLEtendueLesEvenementsAnnules() {
    EvenementDePresence pause = pauseDeDupontA(LE_10_MAI_2026_A_12H);
    JourneeDeTravail journee = journeeDeDupontOuverteA7H().enregistre(pause).annule(pause.id(), annulationParLeroy());

    assertThat(journee.etendue()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_7H));
  }

  @Test
  void shouldChercherLesFaitsConnusEntreLArriveeEtLeSeuil() {
    assertThat(journeeDeDupontOuverteA7H().fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).contains(
      new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_20H)
    );
    assertThat(journeeDeDupontOuverteA7H().fenetreDeRecherche(AMPLITUDE_MAXIMALE_10H)).contains(
      new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H)
    );
  }

  @Test
  void shouldNeRienChercherSansArrivee() {
    assertThat(JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT).fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  /**
   * E2 : lundi sans depart, lu mardi. La journee abandonnee se ferme au dernier fait connu, la fin de l'OF 43 a 16:00,
   * et seule la derniere fenetre est presumee.
   */
  @Test
  void shouldFermerUneJourneeAbandonneeASaFinPresumee() {
    assertThat(
      journeeDeLundiSansDepart().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))
    ).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H)),
      new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H), true)
    );
  }

  @Test
  void shouldPresumerLaFinAuDernierEvenementDePresenceSansPointage() {
    assertThat(journeeDeLundiSansDepart().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.empty())).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H)),
      new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_13H), true)
    );
  }

  @Test
  void shouldIgnorerUnPointageAnterieurAuDernierFaitDePresence() {
    assertThat(journeeDeLundiSansDepart().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_9H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_13H), true));
  }

  @Test
  void shouldIgnorerUnPointageHorsDeLaFenetreDeRecherche() {
    assertThat(journeeDeLundiSansDepart().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_11_MAI_2026_A_7H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_13H), true));
  }

  @Test
  void shouldLaisserOuverteUneJourneeNonAbandonnee() {
    assertThat(journeeDeLundiSansDepart().fenetresA(LE_10_MAI_2026_A_17H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.empty()));
  }

  @Test
  void shouldLaisserOuverteUneJourneeLueAuSeuilPile() {
    assertThat(journeeDeLundiSansDepart().fenetresA(LE_10_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H)))
      .last()
      .matches(FenetreDePresence::estOuverte);
  }

  @Test
  void shouldNeRienPresumerDUneJourneeAbandonneeEnPause() {
    JourneeDeTravail enPause = journeeDeDupontOuverteA7H().enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H));

    assertThat(enPause.fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H))
    );
  }

  @Test
  void shouldNeRienPresumerDUneJourneeFermee() {
    assertThat(
      journeeDeDupontDe7HA17HAvecPauseDeMidi().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))
    ).isEqualTo(journeeDeDupontDe7HA17HAvecPauseDeMidi().fenetres());
  }

  /**
   * Un fait de presence regularise au-dela du seuil reste un fait connu : la fin presumee ne le precede jamais.
   */
  @Test
  void shouldNeJamaisPresumerAvantLeDernierFaitDePresence() {
    JourneeDeTravail reprisTard = journeeDeDupontOuverteA7H()
      .enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H))
      .enregistre(repriseDeDupontA(LE_10_MAI_2026_A_20H.plusSeconds(3600)));

    assertThat(reprisTard.fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_20H.plusSeconds(3600), Optional.of(LE_10_MAI_2026_A_20H.plusSeconds(3600)), true));
  }

  private static JourneeDeTravail journeeDeLundiSansDepart() {
    return journeeDeDupontOuverteA7H().enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H)).enregistre(repriseDeDupontA(LE_10_MAI_2026_A_13H));
  }
}

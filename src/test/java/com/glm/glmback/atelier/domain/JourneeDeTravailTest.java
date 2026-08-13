package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
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
}

package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Strategie « bornes de fin de journee », lot 6 : une journee est en anomalie quand elle est abandonnee sans depart,
 * ou fermee au-dela du seuil. Le seuil vaut 13 h sauf mention contraire.
 */
@UnitTest
class AnomalieDePresenceTest {

  @Test
  void shouldNotBuildWithoutType() {
    JourneeDeTravail journee = journeeDeDupontOuverteA7H();

    assertThatThrownBy(() -> new AnomalieDePresence(null, journee))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutJournee() {
    assertThatThrownBy(() -> new AnomalieDePresence(TypeDAnomalie.JOURNEE_SANS_DEPART, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journee");
  }

  /**
   * E2 : lundi sans depart, lu mardi.
   */
  @Test
  void shouldSignalerUneJourneeAbandonneeSansDepart() {
    JourneeDeTravail lundi = journeeDeDupontOuverteA7H();

    Optional<AnomalieDePresence> anomalie = AnomalieDePresence.de(lundi, LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H);

    assertThat(anomalie).contains(new AnomalieDePresence(TypeDAnomalie.JOURNEE_SANS_DEPART, lundi));
    assertThat(anomalie.orElseThrow().arrivee()).isEqualTo(LE_10_MAI_2026_A_7H);
    assertThat(anomalie.orElseThrow().depart()).isEmpty();
    assertThat(anomalie.orElseThrow().amplitude()).isEmpty();
  }

  @Test
  void shouldNeRienSignalerDUneJourneeEncoreEnCours() {
    assertThat(AnomalieDePresence.de(journeeDeDupontOuverteA7H(), LE_10_MAI_2026_A_17H, AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  @Test
  void shouldNeRienSignalerAuSeuilPile() {
    assertThat(AnomalieDePresence.de(journeeDeDupontOuverteA7H(), LE_10_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  /**
   * E4 : un retour en soiree absorbe donne une journee fermee de 16 h, au-dela du seuil.
   */
  @Test
  void shouldSignalerUneAmplitudeExcessive() {
    JourneeDeTravail seize = journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_10_MAI_2026_A_7H.plus(Duration.ofHours(16))));

    AnomalieDePresence anomalie = AnomalieDePresence.de(seize, LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H).orElseThrow();

    assertThat(anomalie.type()).isEqualTo(TypeDAnomalie.AMPLITUDE_EXCESSIVE);
    assertThat(anomalie.depart()).contains(LE_10_MAI_2026_A_7H.plus(Duration.ofHours(16)));
    assertThat(anomalie.amplitude()).contains(Duration.ofHours(16));
  }

  @Test
  void shouldNeRienSignalerDUneJourneeFermeeAuSeuilPile() {
    JourneeDeTravail treize = journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_10_MAI_2026_A_20H));

    assertThat(AnomalieDePresence.de(treize, LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  @Test
  void shouldNeRienSignalerDUneJourneeFermeeSousLeSeuil() {
    assertThat(AnomalieDePresence.de(journeeDeDupontDe7HA17HAvecPauseDeMidi(), LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  /**
   * E2 regularise : le depart saisi a 17:00 fait disparaitre l'anomalie.
   */
  @Test
  void shouldDisparaitreApresRegularisationDuDepart() {
    JourneeDeTravail regularisee = journeeDeDupontOuverteA7H().enregistre(departRegulariseParLeroyA(LE_10_MAI_2026_A_17H));

    assertThat(AnomalieDePresence.de(regularisee, LE_11_MAI_2026_A_9H15, AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  @Test
  void shouldAppliquerLeSeuilDonne() {
    assertThat(AnomalieDePresence.de(journeeDeDupontDe7HA17HAvecPauseDeMidi(), LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_10H))
      .map(AnomalieDePresence::type)
      .isEmpty();
    JourneeDeTravail onze = journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_10_MAI_2026_A_17H.plus(Duration.ofHours(1))));

    assertThat(AnomalieDePresence.de(onze, LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_10H))
      .map(AnomalieDePresence::type)
      .contains(TypeDAnomalie.AMPLITUDE_EXCESSIVE);
  }

  @Test
  void shouldNeRienSignalerDUneJourneeVide() {
    assertThat(
      AnomalieDePresence.de(
        JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_DUPONT),
        LE_11_MAI_2026_A_9H,
        AMPLITUDE_MAXIMALE_13H
      )
    ).isEmpty();
  }
}

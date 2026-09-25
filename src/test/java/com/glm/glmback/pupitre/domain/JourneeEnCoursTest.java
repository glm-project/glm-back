package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeEnCoursTest {

  @Test
  void shouldNotBuildWithoutEtat() {
    assertThatThrownBy(() -> new JourneeEnCours(null, LE_10_MAI_2026_A_7H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("etat de presence");
  }

  @Test
  void shouldNotBuildWithoutArrivee() {
    assertThatThrownBy(() -> new JourneeEnCours(EtatDePresence.PRESENT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("arrivee");
  }

  @Test
  void shouldResterPresentJusquALArriveePlusLeSeuil() {
    PresenceDuPupitre presence = new JourneeEnCours(EtatDePresence.PRESENT, LE_10_MAI_2026_A_7H).a(
      LE_10_MAI_2026_A_9H,
      AMPLITUDE_MAXIMALE_13H
    );

    assertThat(presence).isEqualTo(new PresenceDuPupitre(EtatDePresence.PRESENT, Optional.of(LE_10_MAI_2026_A_20H)));
  }

  @Test
  void shouldGarderLaPauseSousLeSeuil() {
    PresenceDuPupitre presence = new JourneeEnCours(EtatDePresence.EN_PAUSE, LE_10_MAI_2026_A_7H).a(
      LE_10_MAI_2026_A_12H,
      AMPLITUDE_MAXIMALE_13H
    );

    assertThat(presence).isEqualTo(new PresenceDuPupitre(EtatDePresence.EN_PAUSE, Optional.of(LE_10_MAI_2026_A_20H)));
  }

  @Test
  void shouldResterPresentAuSeuilPile() {
    PresenceDuPupitre presence = new JourneeEnCours(EtatDePresence.PRESENT, LE_10_MAI_2026_A_7H).a(
      LE_10_MAI_2026_A_20H,
      AMPLITUDE_MAXIMALE_13H
    );

    assertThat(presence.etat()).isEqualTo(EtatDePresence.PRESENT);
  }

  @Test
  void shouldEtreAbsentUneFoisLaJourneeAbandonnee() {
    PresenceDuPupitre presence = new JourneeEnCours(EtatDePresence.EN_PAUSE, LE_10_MAI_2026_A_7H).a(
      LE_10_MAI_2026_A_20H.plusSeconds(1),
      AMPLITUDE_MAXIMALE_13H
    );

    assertThat(presence).isEqualTo(PresenceDuPupitre.absente());
  }
}

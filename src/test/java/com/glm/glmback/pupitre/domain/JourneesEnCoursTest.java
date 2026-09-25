package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneesEnCoursTest {

  @Test
  void shouldNotBuildWithoutJournees() {
    assertThatThrownBy(() -> new JourneesEnCours(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journees");
  }

  @Test
  void shouldRendreLaPresenceDeChaqueOperateurAUnInstant() {
    JourneesEnCours journees = new JourneesEnCours(
      Map.of(
        OPERATEUR_ID_DUPONT,
        new JourneeEnCours(EtatDePresence.PRESENT, LE_10_MAI_2026_A_7H),
        OPERATEUR_ID_MARTIN,
        new JourneeEnCours(EtatDePresence.PRESENT, LE_10_MAI_2026_A_7H.minusSeconds(86_400))
      )
    );

    PresencesDesOperateurs presences = journees.a(LE_10_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H);

    assertThat(presences.de(OPERATEUR_ID_DUPONT)).isEqualTo(PRESENCE_PRESENTE_JUSQU_A_20H);
    assertThat(presences.de(OPERATEUR_ID_MARTIN)).isEqualTo(PresenceDuPupitre.absente());
  }
}

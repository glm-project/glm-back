package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class PresencesDesOperateursTest {

  @Test
  void shouldNotBuildWithoutPresences() {
    assertThatThrownBy(() -> new PresencesDesOperateurs(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("presences");
  }

  @Test
  void shouldRendreLEtatDeLaJourneeEnCoursDeLOperateur() {
    PresencesDesOperateurs presences = new PresencesDesOperateurs(Map.of(OPERATEUR_ID_DUPONT, PRESENCE_PRESENTE_JUSQU_A_20H));

    assertThat(presences.de(OPERATEUR_ID_DUPONT)).isEqualTo(PRESENCE_PRESENTE_JUSQU_A_20H);
  }

  @Test
  void shouldTenirPourAbsentUnOperateurSansJourneeEnCours() {
    PresencesDesOperateurs presences = new PresencesDesOperateurs(Map.of(OPERATEUR_ID_DUPONT, PRESENCE_PRESENTE_JUSQU_A_20H));

    assertThat(presences.de(OPERATEUR_ID_MARTIN)).isEqualTo(PresenceDuPupitre.absente());
  }
}

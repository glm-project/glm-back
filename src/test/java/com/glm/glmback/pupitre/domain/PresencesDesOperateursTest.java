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
    PresencesDesOperateurs presences = new PresencesDesOperateurs(Map.of(OPERATEUR_ID_DUPONT, EtatDePresence.EN_PAUSE));

    assertThat(presences.de(OPERATEUR_ID_DUPONT)).isEqualTo(EtatDePresence.EN_PAUSE);
  }

  /**
   * Un operateur qu'aucune journee en cours ne nomme est absent, et le referentiel le rend quand meme : c'est la liste
   * des operateurs designables, pas celle des operateurs presents.
   */
  @Test
  void shouldTenirPourAbsentUnOperateurSansJourneeEnCours() {
    PresencesDesOperateurs presences = new PresencesDesOperateurs(Map.of(OPERATEUR_ID_DUPONT, EtatDePresence.PRESENT));

    assertThat(presences.de(OPERATEUR_ID_MARTIN)).isEqualTo(EtatDePresence.ABSENT);
  }
}

package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CleDActiviteTest {

  @Test
  void shouldNotBuildWithoutOperateur() {
    Optional<PosteDeTravailId> poste = Optional.of(POSTE_ID_FRAISEUSE_1);

    assertThatThrownBy(() -> new CleDActivite(null, poste))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() -> new CleDActivite(OPERATEUR_ID_DUPONT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  /**
   * Une entreprise sans parc machine laisse le poste vide et retrouve une activite unique par operateur.
   */
  @Test
  void shouldBuildSansPosteDeTravail() {
    CleDActivite activite = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty());

    assertThat(activite.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(activite.poste()).isEmpty();
  }
}

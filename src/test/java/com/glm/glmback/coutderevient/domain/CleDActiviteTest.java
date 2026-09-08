package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CleDActiviteTest {

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new CleDActivite(null, Optional.of(POSTE_ID_FRAISEUSE)))
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
   * Une activite sans poste reste une activite : une entreprise sans parc machine retrouve une cle unique par
   * operateur, et un diviseur de un.
   */
  @Test
  void shouldBuildWithoutPosteDeTravail() {
    assertThat(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty()).poste()).isEmpty();
  }
}

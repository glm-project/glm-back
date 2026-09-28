package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeLElementTest {

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() -> new PosteDeLElement(null, Optional.of(NATURE_FRAISAGE)))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new PosteDeLElement(POSTE_CONNU_DMU_50, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }
}

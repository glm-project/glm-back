package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class FicheDElementTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new FicheDElement(null, Optional.empty(), Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'element de fabrication");
  }

  @Test
  void shouldNotBuildWithoutReference() {
    assertThatThrownBy(() -> new FicheDElement(ELEMENT_ID_CARTER, null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    assertThatThrownBy(() -> new FicheDElement(ELEMENT_ID_CARTER, Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }
}

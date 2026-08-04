package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class TitreTest {

  @Test
  void shouldNotBuildWithoutTitre() {
    assertThatThrownBy(() -> new Titre(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldNotBuildWithBlankTitre() {
    assertThatThrownBy(() -> new Titre(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldNotBuildWithTooLongTitre() {
    String tooLong = "a".repeat(256);

    assertThatThrownBy(() -> new Titre(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldGetValueFromValidTitre() {
    assertThat(titreAssemblageCarter().value()).isEqualTo("Assemblage carter");
  }
}

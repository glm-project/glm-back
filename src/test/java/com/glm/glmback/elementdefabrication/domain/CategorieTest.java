package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class CategorieTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Categorie(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @ParameterizedTest
  @ValueSource(strings = { "of", "ORDRE_DE_FABRICATION", "MOULE1", "ABCDEFGHIJK" })
  void shouldNotBuildOutOfPrefixePattern(String value) {
    assertThatThrownBy(() -> new Categorie(value))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldGetValueFromValidCategorie() {
    assertThat(CATEGORIE_MOULE.value()).isEqualTo("MOULE");
  }
}

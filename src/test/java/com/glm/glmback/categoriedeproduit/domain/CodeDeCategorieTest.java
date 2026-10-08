package com.glm.glmback.categoriedeproduit.domain;

import static com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class CodeDeCategorieTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new CodeDeCategorie(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("code");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new CodeDeCategorie(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("code");
  }

  @ParameterizedTest
  @ValueSource(strings = { "moule", "Moule", "MOULE1", "MOULE-A", "ÉBAUCHE", "ABCDEFGHIJK" })
  void shouldNotBuildOutOfPattern(String value) {
    assertThatThrownBy(() -> new CodeDeCategorie(value))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("code");
  }

  @Test
  void shouldBuildWithTenLetters() {
    assertThat(new CodeDeCategorie("ABCDEFGHIJ").value()).isEqualTo("ABCDEFGHIJ");
  }

  @Test
  void shouldGetValueFromValidCode() {
    assertThat(CODE_OF.value()).isEqualTo("OF");
  }
}

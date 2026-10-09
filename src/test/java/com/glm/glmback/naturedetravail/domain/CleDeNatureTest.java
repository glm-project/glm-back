package com.glm.glmback.naturedetravail.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class CleDeNatureTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new CleDeNature(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cle");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new CleDeNature(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cle");
  }

  @ParameterizedTest
  @ValueSource(strings = { "Soudage", "SOUDAGE", "soudage", "Soudâge", "  soudage  " })
  void shouldIgnoreCaseAccentsAndSurroundingSpaces(String libelle) {
    assertThat(new LibelleDeNature(libelle).cle()).isEqualTo(new CleDeNature("soudage"));
  }

  @Test
  void shouldReduceInnerSpacesToOne() {
    assertThat(new LibelleDeNature("Soudage   TIG").cle()).isEqualTo(new CleDeNature("soudage tig"));
  }

  @Test
  void shouldKeepPunctuation() {
    assertThat(new LibelleDeNature("Électro-Érosion").cle()).isEqualTo(new CleDeNature("electro-erosion"));
  }
}

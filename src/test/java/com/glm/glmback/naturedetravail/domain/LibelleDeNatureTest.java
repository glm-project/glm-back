package com.glm.glmback.naturedetravail.domain;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class LibelleDeNatureTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new LibelleDeNature(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new LibelleDeNature("  "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithMoreThanFiftyCharacters() {
    assertThatThrownBy(() -> new LibelleDeNature("a".repeat(51)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldStripSurroundingSpaces() {
    assertThat(new LibelleDeNature("  Soudage ").value()).isEqualTo("Soudage");
  }

  @Test
  void shouldMeasureLengthOnceStripped() {
    assertThat(new LibelleDeNature(" " + "a".repeat(50) + " ").value()).hasSize(50);
  }

  @Test
  void shouldKeepCaseAndAccents() {
    assertThat(new LibelleDeNature("Électro-érosion").value()).isEqualTo("Électro-érosion");
  }

  @Test
  void shouldGiveItsKey() {
    assertThat(LIBELLE_SOUDAGE.cle()).isEqualTo(new CleDeNature("soudage"));
  }
}

package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class LibelleTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Libelle(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Libelle(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(101);

    assertThatThrownBy(() -> new Libelle(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldGetValueFromValidLibelle() {
    assertThat(LIBELLE_TOUR_1.value()).isEqualTo("Tour 1");
  }
}

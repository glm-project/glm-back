package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class LibelleDePosteTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new LibelleDePoste(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new LibelleDePoste(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(101);

    assertThatThrownBy(() -> new LibelleDePoste(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldGetValueFromValidLibelle() {
    assertThat(LIBELLE_TOUR_1.value()).isEqualTo("Tour 1");
  }

  @Test
  void shouldOrderLibellesAlphabetically() {
    assertThat(LIBELLE_POSTE_DE_SOUDURE).isLessThan(LIBELLE_TOUR_1);
    assertThat(LIBELLE_TOUR_1).isGreaterThan(LIBELLE_POSTE_DE_SOUDURE);
  }
}

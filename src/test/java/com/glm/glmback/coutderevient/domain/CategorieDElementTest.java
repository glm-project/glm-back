package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class CategorieDElementTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new CategorieDElement(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie de l'element");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new CategorieDElement(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie de l'element");
  }

  @Test
  void shouldKeepAnyCategorieOfTheEntreprise() {
    assertThat(new CategorieDElement("MOULE")).isEqualTo(CATEGORIE_MOULE);
  }
}

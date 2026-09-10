package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementValoriseTest {

  @Test
  void shouldNotBuildWithoutElement() {
    assertThatThrownBy(() -> new ElementValorise(null, NOM_D_ELEMENT_OF_2026_000001, TypeDElement.ORDRE_DE_FABRICATION))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("element");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new ElementValorise(ELEMENT_ID_OF, null, TypeDElement.ORDRE_DE_FABRICATION))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new ElementValorise(ELEMENT_ID_OF, NOM_D_ELEMENT_OF_2026_000001, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldBuildWithElementNomAndType() {
    assertThat(ELEMENT_VALORISE_OF.nom()).isEqualTo(NOM_D_ELEMENT_OF_2026_000001);
  }
}

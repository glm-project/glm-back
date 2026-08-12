package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementEngageTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new ElementEngage(null, NOM_OF_2026_000042, TypeDElementEngage.ORDRE_DE_FABRICATION))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'element engage");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new ElementEngage(ELEMENT_OF_2026_000042, null, TypeDElementEngage.ORDRE_DE_FABRICATION))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new ElementEngage(ELEMENT_OF_2026_000042, NOM_OF_2026_000042, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type de l'element");
  }

  @Test
  void shouldCopierLIdentiteDeLElement() {
    ElementEngage element = elementEngageOf2026000042();

    assertThat(element.id()).isEqualTo(ELEMENT_OF_2026_000042);
    assertThat(element.nom()).isEqualTo(NOM_OF_2026_000042);
    assertThat(element.type()).isEqualTo(TypeDElementEngage.ORDRE_DE_FABRICATION);
  }
}

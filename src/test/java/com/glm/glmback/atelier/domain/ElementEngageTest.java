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
    assertThatThrownBy(() -> new ElementEngage(null, NOM_OF_2026_000042, CATEGORIE_OF))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'element engage");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new ElementEngage(ELEMENT_OF_2026_000042, null, CATEGORIE_OF))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() -> new ElementEngage(ELEMENT_OF_2026_000042, NOM_OF_2026_000042, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie de l'element");
  }

  @Test
  void shouldCopierLIdentiteDeLElement() {
    ElementEngage element = elementEngageOf2026000042();

    assertThat(element.id()).isEqualTo(ELEMENT_OF_2026_000042);
    assertThat(element.nom()).isEqualTo(NOM_OF_2026_000042);
    assertThat(element.categorie()).isEqualTo(CATEGORIE_OF);
  }
}

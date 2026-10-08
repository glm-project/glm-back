package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementEngageTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new ElementEngage(null, NOM_PRD_2026_000015, CATEGORIE_MOULE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'element de fabrication");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new ElementEngage(ELEMENT_ID_CARTER, null, CATEGORIE_MOULE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() -> new ElementEngage(ELEMENT_ID_CARTER, NOM_PRD_2026_000015, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie de l'element");
  }
}

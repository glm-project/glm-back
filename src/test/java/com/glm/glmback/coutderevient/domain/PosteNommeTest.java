package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteNommeTest {

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() -> new PosteNomme(null, new LibelleDePoste("DMG DMU 50")))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new PosteNomme(POSTE_ID_FRAISEUSE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }
}

package com.glm.glmback.postedetravail.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
@SuppressWarnings("removal")
class NatureChoisieTest {

  @Test
  void shouldNotChooseByIdentifiantWithoutId() {
    assertThatThrownBy(() -> new NatureChoisie.ParIdentifiant(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de travail");
  }

  @Test
  void shouldNotChooseByLibelleWithoutLibelle() {
    assertThatThrownBy(() -> new NatureChoisie.ParLibelle(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }
}

package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDuPosteTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new NatureDuPoste(null, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de travail");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new NatureDuPoste(NATURE_DE_TRAVAIL_ID_TOURNAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldBuildNatureDuPoste() {
    NatureDuPoste nature = new NatureDuPoste(
      new NatureDeTravailId(UUID.fromString("7c0e6a52-1f3d-4b8e-9d21-5a4b3c2d1e01")),
      new NatureDeTravail("tournage")
    );

    assertThat(nature.id()).isEqualTo(NATURE_DE_TRAVAIL_ID_TOURNAGE);
    assertThat(nature.libelle()).isEqualTo(NATURE_TOURNAGE);
  }
}

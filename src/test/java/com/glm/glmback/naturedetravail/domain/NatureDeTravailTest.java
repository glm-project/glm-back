package com.glm.glmback.naturedetravail.domain;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDeTravailTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new NatureDeTravail(null, LIBELLE_SOUDAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new NatureDeTravail(NATURE_DE_TRAVAIL_ID_SOUDAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldBuildNatureDeTravail() {
    NatureDeTravail nature = new NatureDeTravail(
      new NatureDeTravailId(UUID.fromString("5b1d3c1e-7a51-4f0e-9a4e-0f6c1d2b3a01")),
      new LibelleDeNature("Soudage")
    );

    assertThat(nature.id()).isEqualTo(NATURE_DE_TRAVAIL_ID_SOUDAGE);
    assertThat(nature.libelle()).isEqualTo(LIBELLE_SOUDAGE);
  }
}

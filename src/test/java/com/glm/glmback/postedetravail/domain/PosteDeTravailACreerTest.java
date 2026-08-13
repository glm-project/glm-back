package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailACreerTest {

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new PosteDeTravailACreer(null, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new PosteDeTravailACreer(LIBELLE_TOUR_1, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    PosteDeTravailACreer aCreer = new PosteDeTravailACreer("Tour 1", "tournage");

    assertThat(aCreer.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(aCreer.nature()).isEqualTo(NATURE_TOURNAGE);
  }
}

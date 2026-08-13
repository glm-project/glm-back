package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailAModifierTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteDeTravailAModifier(null, LIBELLE_TOUR_1, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, null, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    PosteDeTravailAModifier aModifier = new PosteDeTravailAModifier(id, "Tour 1", "tournage");

    assertThat(aModifier.id()).isEqualTo(id);
    assertThat(aModifier.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(aModifier.nature()).isEqualTo(NATURE_TOURNAGE);
  }
}

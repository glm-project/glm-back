package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailAModifierTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteDeTravailAModifier(null, LIBELLE_TOUR_1, NATURE_TOURNAGE, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, null, NATURE_TOURNAGE, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldNotBuildWithoutCoutHoraire() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, NATURE_TOURNAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    PosteDeTravailAModifier aModifier = new PosteDeTravailAModifier(id, "Tour 1", "tournage", new BigDecimal("45.50"));

    assertThat(aModifier.id()).isEqualTo(id);
    assertThat(aModifier.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(aModifier.nature()).isEqualTo(NATURE_TOURNAGE);
    assertThat(aModifier.coutHoraire()).contains(COUT_HORAIRE_45_50);
  }

  @Test
  void shouldAcceptAbsentCoutHoraire() {
    PosteDeTravailAModifier aModifier = new PosteDeTravailAModifier(PosteDeTravailId.newId(), "Tour 1", "tournage", null);

    assertThat(aModifier.coutHoraire()).isEmpty();
  }
}

package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteDeTravail(null, LIBELLE_TOUR_1, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravail(id, null, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravail(id, LIBELLE_TOUR_1, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldBuildPosteDeTravail() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    PosteDeTravail poste = new PosteDeTravail(id, LIBELLE_TOUR_1, NATURE_TOURNAGE);

    assertThat(poste.id()).isEqualTo(id);
    assertThat(poste.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(poste.nature()).isEqualTo(NATURE_TOURNAGE);
  }

  @Test
  void shouldKeepIdentityWhenRevising() {
    PosteDeTravail poste = posteDeTravailTour1();

    PosteDeTravail revise = poste.revise(LIBELLE_FRAISEUSE_1, NATURE_FRAISAGE);

    assertThat(revise.id()).isEqualTo(poste.id());
    assertThat(revise.libelle()).isEqualTo(LIBELLE_FRAISEUSE_1);
    assertThat(revise.nature()).isEqualTo(NATURE_FRAISAGE);
  }
}

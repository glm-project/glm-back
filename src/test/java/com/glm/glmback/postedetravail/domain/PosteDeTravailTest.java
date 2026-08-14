package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteDeTravail(null, LIBELLE_TOUR_1, NATURE_TOURNAGE, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravail(id, null, NATURE_TOURNAGE, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravail(id, LIBELLE_TOUR_1, null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldNotBuildWithoutCoutHoraire() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravail(id, LIBELLE_TOUR_1, NATURE_TOURNAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldBuildPosteDeTravail() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    PosteDeTravail poste = new PosteDeTravail(id, LIBELLE_TOUR_1, NATURE_TOURNAGE, Optional.empty());

    assertThat(poste.id()).isEqualTo(id);
    assertThat(poste.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(poste.nature()).isEqualTo(NATURE_TOURNAGE);
    assertThat(poste.coutHoraire()).isEmpty();
  }

  @Test
  void shouldBuildPosteDeTravailFromRawValues() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    PosteDeTravail poste = PosteDeTravail.builder()
      .id(id)
      .libelle(LIBELLE_TOUR_1)
      .nature(NATURE_TOURNAGE)
      .coutHoraire(new BigDecimal("45.50"));

    assertThat(poste.id()).isEqualTo(id);
    assertThat(poste.coutHoraire()).contains(COUT_HORAIRE_45_50);
  }

  @Test
  void shouldBuildPosteDeTravailWithoutCoutHoraireFromRawValues() {
    PosteDeTravail poste = PosteDeTravail.builder()
      .id(PosteDeTravailId.newId())
      .libelle(LIBELLE_TOUR_1)
      .nature(NATURE_TOURNAGE)
      .coutHoraire(null);

    assertThat(poste.coutHoraire()).isEmpty();
  }

  @Test
  void shouldKeepIdentityWhenRevising() {
    PosteDeTravail poste = posteDeTravailTour1();

    PosteDeTravail revise = poste.revise(LIBELLE_FRAISEUSE_1, NATURE_FRAISAGE, Optional.of(COUT_HORAIRE_60));

    assertThat(revise.id()).isEqualTo(poste.id());
    assertThat(revise.libelle()).isEqualTo(LIBELLE_FRAISEUSE_1);
    assertThat(revise.nature()).isEqualTo(NATURE_FRAISAGE);
    assertThat(revise.coutHoraire()).contains(COUT_HORAIRE_60);
  }
}

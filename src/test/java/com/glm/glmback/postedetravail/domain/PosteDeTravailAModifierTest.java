package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailAModifierTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteDeTravailAModifier(null, LIBELLE_TOUR_1, NATURE_DE_TRAVAIL_ID_TOURNAGE, COUT_HORAIRE_45_50))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, null, NATURE_DE_TRAVAIL_ID_TOURNAGE, COUT_HORAIRE_45_50))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, null, COUT_HORAIRE_45_50))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de travail");
  }

  @Test
  void shouldNotBuildWithoutCoutHoraire() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, LIBELLE_TOUR_1, NATURE_DE_TRAVAIL_ID_TOURNAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    PosteDeTravailAModifier aModifier = new PosteDeTravailAModifier(id, "Tour 1", NATURE_DE_TRAVAIL_ID_TOURNAGE, new BigDecimal("45.50"));

    assertThat(aModifier.id()).isEqualTo(id);
    assertThat(aModifier.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(aModifier.nature()).isEqualTo(NATURE_DE_TRAVAIL_ID_TOURNAGE);
    assertThat(aModifier.coutHoraire()).isEqualTo(COUT_HORAIRE_45_50);
  }

  @Test
  void shouldNotBuildWithoutRawCoutHoraire() {
    PosteDeTravailId id = PosteDeTravailId.newId();

    assertThatThrownBy(() -> new PosteDeTravailAModifier(id, "Tour 1", NATURE_DE_TRAVAIL_ID_TOURNAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }
}

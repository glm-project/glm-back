package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailACreerTest {

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new PosteDeTravailACreer(null, NATURE_DE_TRAVAIL_ID_TOURNAGE, COUT_HORAIRE_45_50))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new PosteDeTravailACreer(LIBELLE_TOUR_1, null, COUT_HORAIRE_45_50))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de travail");
  }

  @Test
  void shouldNotBuildWithoutCoutHoraire() {
    assertThatThrownBy(() -> new PosteDeTravailACreer(LIBELLE_TOUR_1, NATURE_DE_TRAVAIL_ID_TOURNAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    PosteDeTravailACreer aCreer = new PosteDeTravailACreer("Tour 1", NATURE_DE_TRAVAIL_ID_TOURNAGE, new BigDecimal("45.50"));

    assertThat(aCreer.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(aCreer.nature()).isEqualTo(NATURE_DE_TRAVAIL_ID_TOURNAGE);
    assertThat(aCreer.coutHoraire()).isEqualTo(COUT_HORAIRE_45_50);
  }

  @Test
  void shouldNotBuildWithoutRawCoutHoraire() {
    assertThatThrownBy(() -> new PosteDeTravailACreer("Tour 1", NATURE_DE_TRAVAIL_ID_TOURNAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }
}

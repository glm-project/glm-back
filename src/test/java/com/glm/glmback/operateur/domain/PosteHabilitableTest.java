package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteHabilitableTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteHabilitable(null, LIBELLE_TOUR_1, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new PosteHabilitable(ID_TOUR_1, null, NATURE_TOURNAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new PosteHabilitable(ID_TOUR_1, LIBELLE_TOUR_1, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldBuildPosteHabilitable() {
    PosteHabilitable poste = new PosteHabilitable(ID_TOUR_1, LIBELLE_TOUR_1, NATURE_TOURNAGE);

    assertThat(poste.id()).isEqualTo(ID_TOUR_1);
    assertThat(poste.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(poste.nature()).isEqualTo(NATURE_TOURNAGE);
  }
}

package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteEnCoursTest {

  @Test
  void shouldNotBuildWithoutActivite() {
    assertThatThrownBy(() -> new ActiviteEnCours(null, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activite");
  }

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() -> new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, null, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldNotBuildWithoutDepuis() {
    assertThatThrownBy(() -> new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("depuis");
  }

  @Test
  void shouldExposerQuiTravailleEtSurQuoi() {
    ActiviteEnCours activite = new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H);

    assertThat(activite.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(activite.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(activite.depuis()).isEqualTo(LE_10_MAI_2026_A_8H);
  }
}

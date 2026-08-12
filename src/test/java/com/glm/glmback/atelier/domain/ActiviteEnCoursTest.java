package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
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
    assertThatThrownBy(() -> new ActiviteEnCours(cleDeFraiseuse1DeDupont(), null, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldNotBuildWithoutDepuis() {
    assertThatThrownBy(() -> new ActiviteEnCours(cleDeFraiseuse1DeDupont(), CategorieDActivite.TRAVAIL, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("depuis");
  }

  @Test
  void shouldReadOperateurEtPosteFromCle() {
    ActiviteEnCours activite = new ActiviteEnCours(cleDeFraiseuse1DeDupont(), CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H);

    assertThat(activite.operateur()).isEqualTo(OPERATEUR_DUPONT);
    assertThat(activite.poste()).contains(POSTE_FRAISEUSE_1);
  }
}

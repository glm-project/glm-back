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
    assertThatThrownBy(() -> new ActiviteEnCours(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activite");
  }

  @Test
  void shouldLireQuiFaitQuoiEtDepuisQuandSurSonActivite() {
    ActiviteEnCours enCours = new ActiviteEnCours(Activite.ouvertePar(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(enCours.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
    assertThat(enCours.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(enCours.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(enCours.categorie()).isEqualTo(CategorieDActivite.NON_CONFORMITE);
    assertThat(enCours.depuis()).isEqualTo(LE_10_MAI_2026_A_8H);
  }
}

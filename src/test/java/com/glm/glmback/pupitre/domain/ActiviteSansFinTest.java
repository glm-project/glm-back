package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteSansFinTest {

  @Test
  void shouldExposerLIdentiteStableEtLesBornesProjetees() {
    ActiviteSansFin activite = travailDeDupontSurFraiseuse1Depuis8H();
    assertThat(activite.ouverture()).isEqualTo(ACTIVITE_ID_88888888);
    assertThat(activite.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(activite.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(activite.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
    assertThat(activite.depuis()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(activite.echeance()).isEqualTo(LE_10_MAI_2026_A_21H);
  }

  @Test
  void shouldRefuserLesValeursObligatoiresManquantes() {
    assertThatThrownBy(() ->
      new ActiviteSansFin(null, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new ActiviteSansFin(ACTIVITE_ID_88888888, null, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new ActiviteSansFin(ACTIVITE_ID_88888888, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, null, LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_21H)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new ActiviteSansFin(ACTIVITE_ID_88888888, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, null, LE_10_MAI_2026_A_21H)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new ActiviteSansFin(ACTIVITE_ID_88888888, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, null)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }
}

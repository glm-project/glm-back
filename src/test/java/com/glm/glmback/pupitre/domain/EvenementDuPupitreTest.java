package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class EvenementDuPupitreTest {

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new EvenementDuPupitre(null, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutActivite() {
    assertThatThrownBy(() -> new EvenementDuPupitre(TypeDePointage.DEBUT, null, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activite");
  }

  @Test
  void shouldNotBuildWithoutDateDeSurvenue() {
    assertThatThrownBy(() -> new EvenementDuPupitre(TypeDePointage.DEBUT, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de survenue");
  }

  @Test
  void shouldPorterLHeureMetierDuGeste() {
    assertThat(debut(LE_10_MAI_2026_A_8H).dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
  }
}

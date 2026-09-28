package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageDAtelierTest {

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() ->
      PointageDAtelier.builder().type(null).poste(Optional.empty()).nature(Optional.empty()).dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() ->
      PointageDAtelier.builder()
        .type(TypeDEvenementDAtelier.DEBUT)
        .poste(null)
        .nature(Optional.empty())
        .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() ->
      PointageDAtelier.builder()
        .type(TypeDEvenementDAtelier.DEBUT)
        .poste(Optional.empty())
        .nature(null)
        .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithoutDateDeSurvenue() {
    assertThatThrownBy(() ->
      PointageDAtelier.builder().type(TypeDEvenementDAtelier.DEBUT).poste(Optional.empty()).nature(Optional.empty()).dateDeSurvenue(null)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de survenue");
  }

  @Test
  void shouldPorterSonTypeSonPosteSaNatureEtSaDate() {
    PointageDAtelier pointage = PointageDAtelier.builder()
      .type(TypeDEvenementDAtelier.NON_CONFORMITE)
      .poste(Optional.of(POSTE_ID_DMU_50))
      .nature(Optional.of(NATURE_FRAISAGE))
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H);

    assertThat(pointage.type()).isEqualTo(TypeDEvenementDAtelier.NON_CONFORMITE);
    assertThat(pointage.poste()).contains(POSTE_ID_DMU_50);
    assertThat(pointage.nature()).contains(NATURE_FRAISAGE);
    assertThat(pointage.dateDeSurvenue()).isEqualTo(LE_LUNDI_11_MAI_2026_A_8H);
  }
}

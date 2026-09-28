package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageDElementTest {

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new PointageDElement(null, ELEMENT_ID_CARTER, Optional.empty(), Optional.empty(), LE_LUNDI_11_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutElement() {
    assertThatThrownBy(() ->
      new PointageDElement(TypeDEvenementDAtelier.DEBUT, null, Optional.empty(), Optional.empty(), LE_LUNDI_11_MAI_2026_A_8H)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("element");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() ->
      new PointageDElement(TypeDEvenementDAtelier.DEBUT, ELEMENT_ID_CARTER, null, Optional.empty(), LE_LUNDI_11_MAI_2026_A_8H)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutDateDeSurvenue() {
    assertThatThrownBy(() ->
      new PointageDElement(TypeDEvenementDAtelier.DEBUT, ELEMENT_ID_CARTER, Optional.empty(), Optional.empty(), null)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de survenue");
  }

  @Test
  void shouldPorterSonGesteSonElementSonPosteEtSaDate() {
    PointageDElement pointage = PointageDElement.builder()
      .type(TypeDEvenementDAtelier.NON_CONFORMITE)
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.of(POSTE_ID_DMU_50))
      .nature(Optional.of(NATURE_FRAISAGE))
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H);

    assertThat(pointage.type()).isEqualTo(TypeDEvenementDAtelier.NON_CONFORMITE);
    assertThat(pointage.element()).isEqualTo(ELEMENT_ID_CARTER);
    assertThat(pointage.poste()).contains(POSTE_ID_DMU_50);
    assertThat(pointage.nature()).contains(NATURE_FRAISAGE);
    assertThat(pointage.dateDeSurvenue()).isEqualTo(LE_LUNDI_11_MAI_2026_A_8H);
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() ->
      new PointageDElement(TypeDEvenementDAtelier.DEBUT, ELEMENT_ID_CARTER, Optional.empty(), null, LE_LUNDI_11_MAI_2026_A_8H)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }
}

package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import org.junit.jupiter.api.Test;

@UnitTest
class HorodatageTest {

  @Test
  void shouldNotBuildWithoutDateDeSurvenue() {
    assertThatThrownBy(() -> new Horodatage(null, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDeSurvenue");
  }

  @Test
  void shouldNotBuildWithoutDateDEnregistrement() {
    assertThatThrownBy(() -> new Horodatage(LE_10_MAI_2026_A_8H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDEnregistrement");
  }

  @Test
  void shouldNotBuildWithDateDEnregistrementBeforeDateDeSurvenue() {
    assertThatThrownBy(() -> new Horodatage(LE_10_MAI_2026_A_9H, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("dateDEnregistrement");
  }

  @Test
  void shouldBuildRegularisationEnregistreeApresLaSurvenue() {
    Horodatage horodatage = new Horodatage(LE_10_MAI_2026_A_8H, LE_11_MAI_2026_A_9H15);

    assertThat(horodatage.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(horodatage.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
    assertThat(horodatage.estDifferee()).isTrue();
  }

  @Test
  void shouldBuildPointageAvecLesDeuxDatesEgales() {
    Horodatage horodatage = Horodatage.saisiA(LE_10_MAI_2026_A_8H);

    assertThat(horodatage.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(horodatage.dateDEnregistrement()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(horodatage.estDifferee()).isFalse();
  }
}

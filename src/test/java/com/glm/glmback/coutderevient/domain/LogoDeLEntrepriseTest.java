package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class LogoDeLEntrepriseTest {

  @Test
  void shouldNotBuildWithoutContenu() {
    assertThatThrownBy(() -> new LogoDeLEntreprise(null, FormatDuLogo.PNG))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("contenu");
  }

  @Test
  void shouldNotBuildWithoutFormat() {
    assertThatThrownBy(() -> new LogoDeLEntreprise(new byte[] { 1 }, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("format");
  }

  @Test
  void shouldNotShareItsContenu() {
    byte[] contenu = { 1, 2 };
    LogoDeLEntreprise logo = new LogoDeLEntreprise(contenu, FormatDuLogo.JPEG);

    contenu[0] = 9;
    logo.contenu()[1] = 9;

    assertThat(logo.contenu()).containsExactly(1, 2);
    assertThat(logo.format()).isEqualTo(FormatDuLogo.JPEG);
  }
}

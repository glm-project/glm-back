package com.glm.glmback.parametrage.domain;

import static com.glm.glmback.parametrage.domain.ImagesFixture.*;
import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class LogoTest {

  @Test
  void shouldNotBuildWithoutContenu() {
    assertThatThrownBy(() -> new Logo(null, FormatDImage.PNG, VERSION_DU_LOGO_0123))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("contenu");
  }

  @Test
  void shouldNotBuildWithoutFormat() {
    assertThatThrownBy(() -> new Logo(pngCarre50(), null, VERSION_DU_LOGO_0123))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("format");
  }

  @Test
  void shouldNotBuildWithoutVersion() {
    assertThatThrownBy(() -> new Logo(pngCarre50(), FormatDImage.PNG, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("version");
  }

  @Test
  void shouldGiveTheSameVersionToTheSameContenu() {
    assertThat(Logo.de(pngCarre50(), FormatDImage.PNG).version()).isEqualTo(Logo.de(pngCarre50(), FormatDImage.PNG).version());
  }

  @Test
  void shouldGiveAnotherVersionToAnotherContenu() {
    assertThat(Logo.de(pngCarre50(), FormatDImage.PNG).version()).isNotEqualTo(Logo.de(pngCarre50Rouge(), FormatDImage.PNG).version());
  }

  @Test
  void shouldNotExposeItsContenuToChanges() {
    byte[] contenu = pngCarre50();
    Logo logo = new Logo(contenu, FormatDImage.PNG, VERSION_DU_LOGO_0123);
    contenu[0] = 0;
    logo.contenu()[1] = 0;

    assertThat(logo.contenu()).isEqualTo(pngCarre50());
  }

  @Test
  void shouldBeEqualByValue() {
    assertThat(logoPngBleu()).isEqualTo(logoPngBleu()).hasSameHashCodeAs(logoPngBleu()).isNotEqualTo(new Object());
  }

  @Test
  void shouldDifferByContenuFormatOrVersion() {
    assertThat(logoPngBleu())
      .isNotEqualTo(new Logo(pngCarre50Rouge(), FormatDImage.PNG, VERSION_DU_LOGO_0123))
      .isNotEqualTo(new Logo(pngCarre50(), FormatDImage.JPEG, VERSION_DU_LOGO_0123))
      .isNotEqualTo(new Logo(pngCarre50(), FormatDImage.PNG, new VersionDuLogo("fedcba9876543210")));
  }
}

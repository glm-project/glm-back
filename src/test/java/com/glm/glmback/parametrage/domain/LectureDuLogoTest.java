package com.glm.glmback.parametrage.domain;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class LectureDuLogoTest {

  private final LogosEnMemoire logos = new LogosEnMemoire();
  private final LectureDuLogo lecture = new LectureDuLogo(logos);

  @Test
  void shouldReadTheCurrentLogoAtItsVersion() {
    logos.update(logoPngBleu());

    assertThat(lecture.enVersion(VERSION_DU_LOGO_0123)).isEqualTo(logoPngBleu());
  }

  @Test
  void shouldNotReadAnotherVersion() {
    logos.update(logoPngBleu());

    assertThatThrownBy(() -> lecture.enVersion(new VersionDuLogo("fedcba9876543210")))
      .isExactlyInstanceOf(LogoIntrouvableException.class)
      .hasMessage("Le logo en version fedcba9876543210 n'est pas le logo courant de l'entreprise");
  }

  @Test
  void shouldNotReadWithoutLogo() {
    assertThatThrownBy(() -> lecture.enVersion(VERSION_DU_LOGO_0123)).isExactlyInstanceOf(LogoIntrouvableException.class);
  }
}

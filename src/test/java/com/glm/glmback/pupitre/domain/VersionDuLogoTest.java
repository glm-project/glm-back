package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import org.junit.jupiter.api.Test;

@UnitTest
class VersionDuLogoTest {

  @Test
  void shouldNotBuildOutOfPattern() {
    assertThatThrownBy(() -> new VersionDuLogo("LOGO"))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("version du logo");
  }
}

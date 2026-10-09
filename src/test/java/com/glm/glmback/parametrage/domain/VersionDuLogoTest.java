package com.glm.glmback.parametrage.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import org.junit.jupiter.api.Test;

@UnitTest
class VersionDuLogoTest {

  @Test
  void shouldNotBuildOutOfPattern() {
    assertThatThrownBy(() -> new VersionDuLogo("0123456789ABCDEF"))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("version");
  }
}

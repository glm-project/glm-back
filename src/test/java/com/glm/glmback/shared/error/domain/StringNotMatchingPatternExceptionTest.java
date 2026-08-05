package com.glm.glmback.shared.error.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class StringNotMatchingPatternExceptionTest {

  @Test
  void shouldGetExceptionInformation() {
    StringNotMatchingPatternException exception = StringNotMatchingPatternException.builder()
      .field("myField")
      .value("value")
      .pattern("^\\d+$")
      .build();

    assertThat(exception.type()).isEqualTo(AssertionErrorType.STRING_NOT_MATCHING_PATTERN);
    assertThat(exception.field()).isEqualTo("myField");
    assertThat(exception.parameters()).containsOnly(entry("pattern", "^\\d+$"));
    assertThat(exception.getMessage()).isEqualTo("The value \"value\" in field \"myField\" must match \"^\\d+$\"");
  }
}

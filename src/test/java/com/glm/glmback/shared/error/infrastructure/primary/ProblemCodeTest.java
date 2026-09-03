package com.glm.glmback.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@UnitTest
class ProblemCodeTest {

  @Test
  void shouldDeriveTheTypeFromTheContextAndTheConstantName() {
    assertThat(problem().getType()).hasToString("urn:glm:erreur:test-context:value-already-used");
  }

  @Test
  void shouldCarryTheStatusAndTheTitleOfTheCode() {
    assertThat(problem().getStatus()).isEqualTo(CONFLICT.value());
    assertThat(problem().getTitle()).isEqualTo("value already used");
  }

  @Test
  void shouldReportTheExceptionMessage() {
    assertThat(problem().getProperties()).containsEntry("message", "49 is already used");
  }

  private static ProblemDetail problem() {
    return TestProblemCode.VALUE_ALREADY_USED.problem(new RuntimeException("49 is already used"));
  }

  private enum TestProblemCode implements ProblemCode {
    VALUE_ALREADY_USED;

    @Override
    public String context() {
      return "test-context";
    }

    @Override
    public HttpStatus status() {
      return CONFLICT;
    }

    @Override
    public String title() {
      return "value already used";
    }
  }
}

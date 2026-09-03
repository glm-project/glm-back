package com.glm.glmback.shared.error.infrastructure.primary;

import static com.glm.glmback.shared.error.infrastructure.primary.ExceptionAdvices.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ExceptionAdviceContract {

  protected abstract Object advice();

  protected abstract Stream<PublishedProblem> erreursPubliees();

  @ParameterizedTest
  @MethodSource("erreursPubliees")
  void shouldPublierUnCodeStable(PublishedProblem attendu) {
    ProblemDetail probleme = translatedBy(advice(), attendu.exception());

    assertThat(probleme.getType()).hasToString(attendu.type());
    assertThat(probleme.getStatus()).isEqualTo(attendu.status().value());
  }

  @Test
  void shouldEprouverChaqueErreurTraduite() {
    assertThat(exceptionsTranslatedBy(advice().getClass())).isEqualTo(exceptionsEprouvees());
  }

  private Set<Class<?>> exceptionsEprouvees() {
    return erreursPubliees().map(PublishedProblem::exception).map(Object::getClass).collect(Collectors.toUnmodifiableSet());
  }
}

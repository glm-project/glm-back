package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.EtatDePresence.*;
import static com.glm.glmback.feuilledetemps.domain.TypeDEvenementDePresence.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class EtatDePresenceTest {

  @ParameterizedTest
  @MethodSource("transitionsAdmises")
  void shouldMoveToNextEtat(EtatDePresence depuis, TypeDEvenementDePresence type, EtatDePresence attendu) {
    assertThat(depuis.apres(type)).contains(attendu);
  }

  @ParameterizedTest
  @MethodSource("transitionsRefusees")
  void shouldRefuseTransition(EtatDePresence depuis, TypeDEvenementDePresence type) {
    assertThat(depuis.apres(type)).isEmpty();
  }

  private static Stream<Arguments> transitionsAdmises() {
    return Stream.of(
      Arguments.of(ABSENT, ARRIVEE, PRESENT),
      Arguments.of(PRESENT, PAUSE, EN_PAUSE),
      Arguments.of(PRESENT, DEPART, ABSENT),
      Arguments.of(EN_PAUSE, REPRISE, PRESENT),
      Arguments.of(EN_PAUSE, DEPART, ABSENT)
    );
  }

  private static Stream<Arguments> transitionsRefusees() {
    return Stream.of(
      Arguments.of(ABSENT, PAUSE),
      Arguments.of(ABSENT, REPRISE),
      Arguments.of(ABSENT, DEPART),
      Arguments.of(PRESENT, ARRIVEE),
      Arguments.of(PRESENT, REPRISE),
      Arguments.of(EN_PAUSE, ARRIVEE),
      Arguments.of(EN_PAUSE, PAUSE)
    );
  }
}

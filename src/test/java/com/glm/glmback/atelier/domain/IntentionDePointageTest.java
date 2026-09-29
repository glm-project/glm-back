package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.IntentionDePointage.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class IntentionDePointageTest {

  @Test
  void shouldOuvrirUneActiviteParUneOuvertureOuUneTransition() {
    assertThat(OUVERTURE.ouvreUneActivite()).isTrue();
    assertThat(TRANSITION.ouvreUneActivite()).isTrue();
    assertThat(FIN.ouvreUneActivite()).isFalse();
  }

  @Test
  void shouldViserUneActiviteParUneTransitionOuUneFin() {
    assertThat(OUVERTURE.viseUneActivite()).isFalse();
    assertThat(TRANSITION.viseUneActivite()).isTrue();
    assertThat(FIN.viseUneActivite()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("typesAdmis")
  void shouldAdmettreLesTypesDeSonIntention(IntentionDePointage intention, TypeDEvenementDAtelier type, boolean admis) {
    assertThat(intention.admet(type)).isEqualTo(admis);
  }

  /**
   * A heure metier egale, la fin passe avant la transition, et la transition avant l'ouverture : jamais la date
   * d'enregistrement ne departage deux gestes simultanes.
   */
  @Test
  void shouldRangerLaFinPuisLaTransitionPuisLOuvertureAHeureEgale() {
    assertThat(FIN.rangAHeureEgale()).isLessThan(TRANSITION.rangAHeureEgale());
    assertThat(TRANSITION.rangAHeureEgale()).isLessThan(OUVERTURE.rangAHeureEgale());
  }

  private static Stream<Arguments> typesAdmis() {
    return Stream.of(
      Arguments.of(OUVERTURE, TypeDEvenementDAtelier.DEBUT, true),
      Arguments.of(OUVERTURE, TypeDEvenementDAtelier.NON_CONFORMITE, true),
      Arguments.of(OUVERTURE, TypeDEvenementDAtelier.FIN, false),
      Arguments.of(TRANSITION, TypeDEvenementDAtelier.DEBUT, true),
      Arguments.of(TRANSITION, TypeDEvenementDAtelier.NON_CONFORMITE, true),
      Arguments.of(TRANSITION, TypeDEvenementDAtelier.FIN, false),
      Arguments.of(FIN, TypeDEvenementDAtelier.DEBUT, false),
      Arguments.of(FIN, TypeDEvenementDAtelier.NON_CONFORMITE, false),
      Arguments.of(FIN, TypeDEvenementDAtelier.FIN, true)
    );
  }
}

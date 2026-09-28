package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.EtatDePresence.*;
import static com.glm.glmback.coutderevient.domain.TypeDEvenementDePresence.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Troisieme ecriture du meme automate, apres celle de l'atelier et celle de la feuille de temps. La duplication est le
 * prix de la frontiere ; les trois jeux de tests portent les memes transitions.
 */
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

  /**
   * La presence alterne l'arrivee et le depart : depuis chaque etat, un seul geste est admis. Meme regle que
   * l'automate de l'atelier, dont celui-ci est la copie.
   */
  @ParameterizedTest
  @EnumSource(EtatDePresence.class)
  void shouldNAdmettreQuUnSeulGesteDepuisChaqueEtat(EtatDePresence depuis) {
    assertThat(Stream.of(TypeDEvenementDePresence.values()).filter(type -> depuis.apres(type).isPresent())).hasSize(1);
  }

  private static Stream<Arguments> transitionsAdmises() {
    return Stream.of(Arguments.of(ABSENT, ARRIVEE, PRESENT), Arguments.of(PRESENT, DEPART, ABSENT));
  }

  private static Stream<Arguments> transitionsRefusees() {
    return Stream.of(Arguments.of(ABSENT, DEPART), Arguments.of(PRESENT, ARRIVEE));
  }
}

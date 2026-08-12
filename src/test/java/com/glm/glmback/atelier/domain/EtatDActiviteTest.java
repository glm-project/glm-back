package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.EtatDActivite.*;
import static com.glm.glmback.atelier.domain.TypeDEvenementDAtelier.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class EtatDActiviteTest {

  @ParameterizedTest
  @MethodSource("transitionsAdmises")
  void shouldMoveToNextEtat(EtatDActivite depuis, TypeDEvenementDAtelier type, EtatDActivite attendu) {
    assertThat(depuis.apres(type)).contains(attendu);
  }

  @ParameterizedTest
  @MethodSource("transitionsRefusees")
  void shouldRefuseTransition(EtatDActivite depuis, TypeDEvenementDAtelier type) {
    assertThat(depuis.apres(type)).isEmpty();
  }

  @Test
  void shouldNotCategoriseEtatAbsente() {
    assertThat(ABSENTE.categorie()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("categories")
  void shouldCategoriseEtatOuverte(EtatDActivite etat, CategorieDActivite attendue) {
    assertThat(etat.categorie()).contains(attendue);
  }

  private static Stream<Arguments> transitionsAdmises() {
    return Stream.of(
      Arguments.of(ABSENTE, DEBUT, EN_COURS),
      Arguments.of(ABSENTE, NON_CONFORMITE, EN_NON_CONFORMITE),
      Arguments.of(EN_COURS, NON_CONFORMITE, EN_NON_CONFORMITE),
      Arguments.of(EN_COURS, FIN, ABSENTE),
      Arguments.of(EN_NON_CONFORMITE, DEBUT, EN_COURS),
      Arguments.of(EN_NON_CONFORMITE, FIN, ABSENTE)
    );
  }

  private static Stream<Arguments> transitionsRefusees() {
    return Stream.of(Arguments.of(ABSENTE, FIN), Arguments.of(EN_COURS, DEBUT), Arguments.of(EN_NON_CONFORMITE, NON_CONFORMITE));
  }

  private static Stream<Arguments> categories() {
    return Stream.of(
      Arguments.of(EN_COURS, CategorieDActivite.TRAVAIL),
      Arguments.of(EN_NON_CONFORMITE, CategorieDActivite.NON_CONFORMITE)
    );
  }
}

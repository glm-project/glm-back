package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.EtatDActivite.*;
import static com.glm.glmback.coutderevient.domain.TypeDEvenementDAtelier.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Le meme automate que celui de l'atelier, rejoue ici. Les deux jeux de tests sont ecrits sur les memes transitions :
 * c'est ce qui tient les deux implementations alignees, avec le scenario Cucumber.
 */
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
  void shouldHaveNoCategorieWhenAbsente() {
    assertThat(ABSENTE.categorie()).isEmpty();
  }

  @Test
  void shouldBeTravailWhenEnCours() {
    assertThat(EN_COURS.categorie()).contains(CategorieDActivite.TRAVAIL);
  }

  /**
   * La categorie se lit sur l'etat atteint, jamais sur le type d'evenement : c'est ce qui fait qu'une reprise de bon
   * travail apres une non conformite se pointe comme un debut.
   */
  @Test
  void shouldBeNonConformiteWhenEnNonConformite() {
    assertThat(EN_NON_CONFORMITE.categorie()).contains(CategorieDActivite.NON_CONFORMITE);
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

  @Test
  void shouldRefuseUnknownTransitionAsEmpty() {
    assertThat(ABSENTE.apres(FIN)).isEqualTo(Optional.empty());
  }
}

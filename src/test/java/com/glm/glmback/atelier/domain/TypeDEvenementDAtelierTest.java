package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class TypeDEvenementDAtelierTest {

  @Test
  void shouldOuvrirUnTravailParUnDebut() {
    assertThat(TypeDEvenementDAtelier.DEBUT.categorie()).contains(CategorieDActivite.TRAVAIL);
  }

  @Test
  void shouldOuvrirUneNonConformiteParUneNonConformite() {
    assertThat(TypeDEvenementDAtelier.NON_CONFORMITE.categorie()).contains(CategorieDActivite.NON_CONFORMITE);
  }

  @Test
  void shouldOuvrirUneActiviteParUnDebutEtUneNonConformiteMaisPasParUneFin() {
    assertThat(TypeDEvenementDAtelier.DEBUT.ouvreUneActivite()).isTrue();
    assertThat(TypeDEvenementDAtelier.NON_CONFORMITE.ouvreUneActivite()).isTrue();
    assertThat(TypeDEvenementDAtelier.FIN.ouvreUneActivite()).isFalse();
  }

  @Test
  void shouldNOuvrirAucuneCategorieParUneFin() {
    assertThat(TypeDEvenementDAtelier.FIN.categorie()).isEmpty();
  }

  @Test
  void shouldNOuvrirAucuneActiviteParUneCorrection() {
    assertThat(TypeDEvenementDAtelier.RETRAIT.ouvreUneActivite()).isFalse();
    assertThat(TypeDEvenementDAtelier.CHANGEMENT_DE_POSTE.ouvreUneActivite()).isFalse();
    assertThat(TypeDEvenementDAtelier.RETRAIT.categorie()).isEmpty();
    assertThat(TypeDEvenementDAtelier.CHANGEMENT_DE_POSTE.categorie()).isEmpty();
  }

  /**
   * Seuls un debut, une non conformite et une fin sont des gestes d'activite : la regle de reception ne juge que ceux-la,
   * et un retrait ou un changement de poste, dates du moment de la correction, ne bornent jamais un pointage.
   */
  @Test
  void shouldDistinguerLesGestesDesCorrections() {
    assertThat(TypeDEvenementDAtelier.DEBUT.estUnGeste()).isTrue();
    assertThat(TypeDEvenementDAtelier.NON_CONFORMITE.estUnGeste()).isTrue();
    assertThat(TypeDEvenementDAtelier.FIN.estUnGeste()).isTrue();
    assertThat(TypeDEvenementDAtelier.RETRAIT.estUnGeste()).isFalse();
    assertThat(TypeDEvenementDAtelier.CHANGEMENT_DE_POSTE.estUnGeste()).isFalse();
  }
}

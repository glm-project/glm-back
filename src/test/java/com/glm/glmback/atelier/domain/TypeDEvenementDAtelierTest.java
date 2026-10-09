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
}

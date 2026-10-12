package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CategorieDActiviteTest {

  @Test
  void shouldOuvrirUnTravailParUnDebutEtUneNonConformiteParUneNonConformite() {
    assertThat(CategorieDActivite.TRAVAIL.typeDOuverture()).isEqualTo(TypeDEvenementDAtelier.DEBUT);
    assertThat(CategorieDActivite.NON_CONFORMITE.typeDOuverture()).isEqualTo(TypeDEvenementDAtelier.NON_CONFORMITE);
  }

  @Test
  void shouldEtreLInverseDeLaCategorieDuType() {
    for (CategorieDActivite categorie : CategorieDActivite.values()) {
      assertThat(categorie.typeDOuverture().categorie()).contains(categorie);
    }
  }
}

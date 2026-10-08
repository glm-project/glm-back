package com.glm.glmback.categoriedeproduit.domain;

import static com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class CategorieDeProduitListeeTest {

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() -> new CategorieDeProduitListee(null, false))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldBuildCategorieDeProduitListee() {
    CategorieDeProduitListee listee = new CategorieDeProduitListee(categorieDeProduitMoule(), true);

    assertThat(listee.categorie()).isEqualTo(categorieDeProduitMoule());
    assertThat(listee.utilisee()).isTrue();
  }
}

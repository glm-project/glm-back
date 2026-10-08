package com.glm.glmback.categoriedeproduit.domain;

import static com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class CategorieDeProduitTest {

  @Test
  void shouldNotBuildWithoutCode() {
    assertThatThrownBy(() -> new CategorieDeProduit(null, RANG_1))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("code");
  }

  @Test
  void shouldNotBuildWithoutRang() {
    assertThatThrownBy(() -> new CategorieDeProduit(CODE_MOULE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("rang");
  }

  @Test
  void shouldBuildCategorieDeProduit() {
    CategorieDeProduit categorie = new CategorieDeProduit(new CodeDeCategorie("MOULE"), new Rang(1));

    assertThat(categorie.code()).isEqualTo(CODE_MOULE);
    assertThat(categorie.rang()).isEqualTo(RANG_1);
  }

  @Test
  void shouldKeepCodeWhenMoved() {
    assertThat(categorieDeProduitMoule().deplace(RANG_2)).isEqualTo(new CategorieDeProduit(CODE_MOULE, RANG_2));
  }
}

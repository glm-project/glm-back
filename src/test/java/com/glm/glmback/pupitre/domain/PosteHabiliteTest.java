package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteHabiliteTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteHabilite(null, LIBELLE_FRAISEUSE_1))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id du poste de travail");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new PosteHabilite(POSTE_ID_FRAISEUSE_1, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldPorterCeQueLeBoutonAffiche() {
    assertThat(POSTE_HABILITE_FRAISEUSE_1.id()).isEqualTo(POSTE_ID_FRAISEUSE_1);
    assertThat(POSTE_HABILITE_FRAISEUSE_1.libelle()).isEqualTo(LIBELLE_FRAISEUSE_1);
  }
}

package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteConnuTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new PosteConnu(null, LIBELLE_FRAISEUSE_1, NATURE_FRAISAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id du poste de travail");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new PosteConnu(POSTE_ID_FRAISEUSE_1, null, NATURE_FRAISAGE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  /**
   * La nature est obligatoire sur un poste du referentiel, alors qu'elle reste facultative sur un evenement : un poste
   * declare sans dire quel travail s'y fait ne servirait a rien.
   */
  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new PosteConnu(POSTE_ID_FRAISEUSE_1, LIBELLE_FRAISEUSE_1, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldGetPosteFromValidPosteConnu() {
    assertThat(POSTE_CONNU_FRAISEUSE_1.id()).isEqualTo(POSTE_ID_FRAISEUSE_1);
    assertThat(POSTE_CONNU_FRAISEUSE_1.libelle()).isEqualTo(LIBELLE_FRAISEUSE_1);
    assertThat(POSTE_CONNU_FRAISEUSE_1.nature()).isEqualTo(NATURE_FRAISAGE);
  }
}

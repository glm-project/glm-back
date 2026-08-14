package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurConnuTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new OperateurConnu(null, NOM_DUPONT, PRENOM_JEAN))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'operateur");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new OperateurConnu(OPERATEUR_ID_DUPONT, null, PRENOM_JEAN))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutPrenom() {
    assertThatThrownBy(() -> new OperateurConnu(OPERATEUR_ID_DUPONT, NOM_DUPONT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldPorterLIdentiteRelueDuReferentiel() {
    OperateurConnu dupont = new OperateurConnu(OPERATEUR_ID_DUPONT, NOM_DUPONT, PRENOM_JEAN);

    assertThat(dupont.id()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(dupont.nom()).isEqualTo(NOM_DUPONT);
    assertThat(dupont.prenom()).isEqualTo(PRENOM_JEAN);
  }
}

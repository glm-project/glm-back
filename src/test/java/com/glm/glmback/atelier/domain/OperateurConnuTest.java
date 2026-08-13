package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
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
  void shouldGetIdentiteFromValidOperateurConnu() {
    assertThat(OPERATEUR_CONNU_DUPONT.id()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(OPERATEUR_CONNU_DUPONT.nom()).isEqualTo(NOM_DUPONT);
    assertThat(OPERATEUR_CONNU_DUPONT.prenom()).isEqualTo(PRENOM_JEAN);
  }
}

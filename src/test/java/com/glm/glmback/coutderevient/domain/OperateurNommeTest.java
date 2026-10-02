package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurNommeTest {

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new OperateurNomme(null, new PrenomDOperateur("Jean"), new NomDOperateur("Dupont")))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutPrenom() {
    assertThatThrownBy(() -> new OperateurNomme(OPERATEUR_ID_DUPONT, null, new NomDOperateur("Dupont")))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new OperateurNomme(OPERATEUR_ID_DUPONT, new PrenomDOperateur("Jean"), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'operateur");
  }
}

package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class CompteRenduDuCoutTest {

  @Test
  void shouldNotBuildWithoutRapport() {
    assertThatThrownBy(() -> new CompteRenduDuCout(null, StatutDeLElement.EN_COURS, FUSEAU_DE_PARIS))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("rapport");
  }

  @Test
  void shouldNotBuildWithoutStatut() {
    assertThatThrownBy(() -> new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, null, FUSEAU_DE_PARIS))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("statut");
  }

  @Test
  void shouldNotBuildWithoutFuseau() {
    assertThatThrownBy(() -> new CompteRenduDuCout(COUT_DE_REVIENT_VIDE, StatutDeLElement.EN_COURS, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fuseau horaire");
  }
}

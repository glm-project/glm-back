package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurCriteriaTest {

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() -> new OperateurCriteria(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste");
  }

  @Test
  void shouldMatchOperateurHabilitatedOnPoste() {
    assertThat(criteresDeTournage().matches(operateurDupont())).isTrue();
  }

  @Test
  void shouldNotMatchOperateurWithoutHabilitationOnPoste() {
    assertThat(criteresDeTournage().matches(operateurMartinSansMatricule())).isFalse();
  }

  @Test
  void shouldMatchAnyOperateurWithoutPoste() {
    assertThat(criteresSansFiltre().matches(operateurMartinSansMatricule())).isTrue();
  }
}

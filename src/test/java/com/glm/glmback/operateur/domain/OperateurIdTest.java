package com.glm.glmback.operateur.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new OperateurId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'operateur");
  }

  @Test
  void shouldBuildNewId() {
    assertThat(OperateurId.newId().uuid()).isNotNull();
  }

  @Test
  void shouldOrderIdsToBreakTiesInPagination() {
    OperateurId premier = new OperateurId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    OperateurId second = new OperateurId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    assertThat(premier).isLessThan(second);
    assertThat(second).isGreaterThan(premier);
  }
}

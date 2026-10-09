package com.glm.glmback.postedetravail.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDeTravailIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new NatureDeTravailId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de travail");
  }

  @Test
  void shouldGenerateDistinctIds() {
    assertThat(NatureDeTravailId.newId()).isNotEqualTo(NatureDeTravailId.newId());
  }
}

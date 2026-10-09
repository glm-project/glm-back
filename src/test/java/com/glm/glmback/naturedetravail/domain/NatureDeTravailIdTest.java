package com.glm.glmback.naturedetravail.domain;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
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

  @Test
  void shouldOrderByUuid() {
    assertThat(NATURE_DE_TRAVAIL_ID_SOUDAGE).isLessThan(NATURE_DE_TRAVAIL_ID_TOURNAGE);
  }
}

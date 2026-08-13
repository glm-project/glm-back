package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailCriteriaTest {

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new PosteDeTravailCriteria(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldMatchPosteOfExpectedNature() {
    assertThat(criteresDeTournage().matches(posteDeTravailTour1())).isTrue();
  }

  @Test
  void shouldNotMatchPosteOfAnotherNature() {
    assertThat(criteresDeTournage().matches(posteDeTravailPosteDeSoudure())).isFalse();
  }

  @Test
  void shouldMatchAnyPosteWithoutNature() {
    assertThat(criteresSansFiltre().matches(posteDeTravailPosteDeSoudure())).isTrue();
  }
}

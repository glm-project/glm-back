package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailCriteriaTest {

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new PosteDeTravailCriteria(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldNotBuildWithoutNatureId() {
    assertThatThrownBy(() -> new PosteDeTravailCriteria(Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de travail");
  }

  @Test
  void shouldMatchPosteOfExpectedNatureId() {
    assertThat(criteresDeLaNatureDeTournage().matches(posteDeTravailTour1())).isTrue();
  }

  @Test
  void shouldNotMatchPosteOfAnotherNatureId() {
    assertThat(criteresDeLaNatureDeTournage().matches(posteDeTravailPosteDeSoudure())).isFalse();
  }

  @Test
  void shouldMatchOnlyWhenBothCriteriaHold() {
    PosteDeTravailCriteria contradictoires = new PosteDeTravailCriteria(
      Optional.of(NATURE_SOUDAGE),
      Optional.of(NATURE_DE_TRAVAIL_ID_TOURNAGE)
    );

    assertThat(contradictoires.matches(posteDeTravailTour1())).isFalse();
    assertThat(contradictoires.matches(posteDeTravailPosteDeSoudure())).isFalse();
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

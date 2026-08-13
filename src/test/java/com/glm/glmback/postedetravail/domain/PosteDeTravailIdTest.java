package com.glm.glmback.postedetravail.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new PosteDeTravailId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id du poste de travail");
  }

  @Test
  void shouldBuildNewId() {
    assertThat(PosteDeTravailId.newId().uuid()).isNotNull();
  }

  @Test
  void shouldOrderIdsToBreakTiesInPagination() {
    PosteDeTravailId premier = new PosteDeTravailId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    PosteDeTravailId second = new PosteDeTravailId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    assertThat(premier).isLessThan(second);
    assertThat(second).isGreaterThan(premier);
  }
}

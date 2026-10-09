package com.glm.glmback.naturedetravail.infrastructure.secondary;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class NaturesSansUsageTest {

  @Test
  void shouldNotSeeNatureAsUsed() {
    assertThat(new NaturesSansUsage().estUtilisee(NATURE_DE_TRAVAIL_ID_SOUDAGE)).isFalse();
  }

  @Test
  void shouldSeeNoUsedNature() {
    assertThat(new NaturesSansUsage().utiliseesParmi(List.of(NATURE_DE_TRAVAIL_ID_SOUDAGE))).isEmpty();
  }
}

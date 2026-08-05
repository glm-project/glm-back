package com.glm.glmback.shared.time.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

@UnitTest
class SystemClockTest {

  private static final Instant LE_10_MAI_2026 = Instant.parse("2026-05-10T08:00:00Z");

  @Test
  void shouldReadInstantFromJavaClock() {
    SystemClock clock = new SystemClock(java.time.Clock.fixed(LE_10_MAI_2026, ZoneOffset.UTC));

    assertThat(clock.now()).isEqualTo(LE_10_MAI_2026);
  }
}

package com.glm.glmback.shared.time.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class ExactInstantConverterTest {

  private final ExactInstantConverter converter = new ExactInstantConverter();

  @ParameterizedTest
  @CsvSource(
    {
      "2026-05-10T08:00:00.123456789Z, 1778400000.123456789",
      "1970-01-01T00:00:00.000000001Z, 0.000000001",
      "1969-12-31T23:59:59.999999999Z, -0.000000001",
      "1969-12-31T23:59:58.123456789Z, -1.876543211",
    }
  )
  void shouldEncoderEtRelireLesSecondesEtNanosecondesExactes(Instant instant, BigDecimal seconds) {
    assertThat(converter.convertToDatabaseColumn(instant)).isEqualByComparingTo(seconds);
    assertThat(converter.convertToEntityAttribute(seconds)).isEqualTo(instant);
  }

  @Test
  void shouldConserverUneDateAbsente() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }
}

package com.glm.glmback.shared.time.infrastructure.secondary;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** An instant as exact decimal seconds since the epoch, keeping SQL comparison and ordering numerical. */
@Converter
public class ExactInstantConverter implements AttributeConverter<Instant, BigDecimal> {

  @Override
  public BigDecimal convertToDatabaseColumn(Instant instant) {
    return instant == null ? null : BigDecimal.valueOf(instant.getEpochSecond()).add(BigDecimal.valueOf(instant.getNano(), 9));
  }

  @Override
  public Instant convertToEntityAttribute(BigDecimal seconds) {
    if (seconds == null) {
      return null;
    }
    long wholeSeconds = seconds.setScale(0, RoundingMode.FLOOR).longValueExact();
    long nanos = seconds.subtract(BigDecimal.valueOf(wholeSeconds)).movePointRight(9).longValueExact();
    return Instant.ofEpochSecond(wholeSeconds, nanos);
  }
}

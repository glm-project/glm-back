package com.glm.glmback.parametrage.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Les formats qu'un logo peut prendre : ceux que tout navigateur et tout PDF affichent sans conversion.
 */
public enum FormatDImage {
  PNG,
  JPEG;

  static Optional<FormatDImage> depuis(String format) {
    return Arrays.stream(values())
      .filter(candidat -> candidat.name().equals(format.toUpperCase(Locale.ROOT)))
      .findFirst();
  }
}

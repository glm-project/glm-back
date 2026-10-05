package com.glm.glmback.atelier.domain.gestionanomalies;

import java.util.Arrays;
import java.util.List;

/** Ce que le gestionnaire doit trancher : une sequence en conflit, et bientot une fin automatique. */
public enum NatureDAnomalie {
  CONFLIT;

  public static NatureDAnomalie of(String valeur) {
    return Arrays.stream(values())
      .filter(nature -> nature.name().equals(valeur))
      .findFirst()
      .orElseThrow(() -> new NatureDAnomalieInvalideException(valeur));
  }

  static List<String> noms() {
    return Arrays.stream(values()).map(Enum::name).toList();
  }
}

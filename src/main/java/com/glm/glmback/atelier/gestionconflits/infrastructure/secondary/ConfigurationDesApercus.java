package com.glm.glmback.atelier.gestionconflits.infrastructure.secondary;

import com.glm.glmback.atelier.gestionconflits.application.ValiditeDesApercus;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("atelier.resolution.apercus")
public record ConfigurationDesApercus(
  String cleActive,
  Map<String, String> cles,
  @DefaultValue("PT15M") Duration validite
) implements ValiditeDesApercus {
  public ConfigurationDesApercus {
    if (cleActive == null || !cleActive.matches("[a-zA-Z0-9_-]{1,32}") || cles == null || !cles.containsKey(cleActive)) {
      throw new IllegalArgumentException("Un trousseau explicite et une cle active sont requis");
    }
    for (var cle : cles.entrySet()) {
      if (!cle.getKey().matches("[a-zA-Z0-9_-]{1,32}") || Base64.getDecoder().decode(cle.getValue()).length != 32) {
        throw new IllegalArgumentException("Chaque cle d'apercu doit etre une cle AES de 256 bits nommee");
      }
    }
    if (validite == null || validite.isZero() || validite.isNegative()) {
      throw new IllegalArgumentException("La validite de l'apercu doit etre positive");
    }
    cles = Map.copyOf(cles);
  }
}

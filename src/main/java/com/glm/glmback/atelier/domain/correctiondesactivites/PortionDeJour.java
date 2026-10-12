package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.LocalDate;

/**
 * La part d'une activite comptee un jour calendaire : coupee aux minuits de l'entreprise, sa duree est un ecart reel
 * entre deux instants, donc la nuit d'un changement d'heure compte sa duree reelle.
 */
public record PortionDeJour(LocalDate jour, Duration duree) {
  public PortionDeJour {
    Assert.notNull("jour", jour);
    Assert.notNull("duree", duree);
  }
}

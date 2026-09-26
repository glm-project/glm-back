package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * Un geste tel que le pupitre l'a envoye : son identifiant, son contenu, et qui l'a saisi.
 */
public record GesteRecu(UUID evenement, GesteEnAttente geste, Auteur auteur) {
  public GesteRecu {
    Assert.notNull("evenement", evenement);
    Assert.notNull("geste", geste);
    Assert.notNull("auteur", auteur);
  }
}

package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Un poste sur lequel l'element a ete travaille dans la semaine, et la nature que l'atelier a figee a la saisie. Un
 * poste requalifie en cours de semaine donne deux couples.
 */
public record PosteDeLElement(PosteConnu poste, Optional<NatureDOperation> nature) {
  public PosteDeLElement {
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
  }
}

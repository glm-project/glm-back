package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce que le referentiel dit d'un element a l'instant de la lecture : sa reference et sa description, relues a chaque
 * releve pour qu'une fiche revisee s'affiche revisee.
 */
public record FicheDElement(ElementId id, Optional<ReferenceDElement> reference, Optional<DescriptionDElement> description) {
  public FicheDElement {
    Assert.notNull("id de l'element de fabrication", id);
    Assert.notNull("reference", reference);
    Assert.notNull("description", description);
  }
}

package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce que le referentiel dit d'un element a l'instant de la lecture : sa reference et sa description, relues a chaque
 * lecture pour qu'une fiche revisee s'affiche revisee. L'atelier n'en copie rien, a la difference du nom et de la
 * categorie figes a l'engagement.
 */
public record FicheDElement(ElementEngageId id, Optional<ReferenceDElement> reference, Optional<DescriptionDElement> description) {
  public FicheDElement {
    Assert.notNull("id de l'element", id);
    Assert.notNull("reference", reference);
    Assert.notNull("description", description);
  }
}

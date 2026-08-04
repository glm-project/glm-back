package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToCreate;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabricationToCreate;
import com.glm.glmback.elementdefabrication.domain.ProduitToCreate;
import com.glm.glmback.elementdefabrication.domain.Titre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record RestCreationElementDeFabrication(
  @NotNull RestTypeElementDeFabrication type,
  @NotBlank @Size(max = 255) String titre,
  @NotBlank @Size(max = 1000) String description
) {
  ElementDeFabricationToCreate toDomain() {
    Titre titreDemande = new Titre(titre);
    Description descriptionDemandee = new Description(description);

    return switch (type) {
      case ORDRE_DE_FABRICATION -> new OrdreDeFabricationToCreate(titreDemande, descriptionDemandee);
      case PRODUIT -> new ProduitToCreate(titreDemande, descriptionDemandee);
    };
  }
}

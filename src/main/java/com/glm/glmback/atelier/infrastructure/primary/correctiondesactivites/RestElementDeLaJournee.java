package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.correctiondesactivites.DescriptionDElement;
import com.glm.glmback.atelier.domain.correctiondesactivites.FicheDElement;
import com.glm.glmback.atelier.domain.correctiondesactivites.ReferenceDElement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import java.util.UUID;

@Schema(
  description = """
  Un element de fabrication, tel que les autres lectures de l'API le rendent : nom et categorie copies a l'engagement du
  suivi, reference et description relues au referentiel et absentes si l'element a ete supprime. Le numero que l'ecran
  affiche est la reference, a defaut le nom ; la designation est la description.
  """
)
record RestElementDeLaJournee(
  @Schema(description = "Identifiant de l'element de fabrication.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Categorie de l'element, copiee a l'engagement.", example = "OF", requiredMode = Schema.RequiredMode.REQUIRED)
  String categorie,
  @Schema(description = "Nom de l'element, copie a l'engagement.", example = "PRD-2026-000015", requiredMode = Schema.RequiredMode.REQUIRED)
  String nom,
  @Schema(description = "Reference de l'element, relue au referentiel.", example = "1015") String reference,
  @Schema(description = "Description de l'element, relue au referentiel.", example = "Carter de pompe") String description
) {
  static RestElementDeLaJournee from(ElementEngage element, Optional<FicheDElement> fiche) {
    return new RestElementDeLaJournee(
      element.id().uuid(),
      element.categorie().value(),
      element.nom().value(),
      fiche.flatMap(FicheDElement::reference).map(ReferenceDElement::value).orElse(null),
      fiche.flatMap(FicheDElement::description).map(DescriptionDElement::value).orElse(null)
    );
  }
}

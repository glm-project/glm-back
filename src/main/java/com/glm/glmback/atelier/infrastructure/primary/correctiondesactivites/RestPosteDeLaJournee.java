package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.PosteConnu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
  description = """
  Un poste de travail du referentiel, relu a chaque lecture, avec sa nature d'operation : celle du referentiel pour un poste
  a choisir, celle figee a la saisie pour le poste d'une activite.
  """
)
record RestPosteDeLaJournee(
  @Schema(description = "Identifiant du poste dans le referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Libelle du poste.", example = "Tour CN 1", requiredMode = Schema.RequiredMode.REQUIRED) String libelle,
  @Schema(
    description = "Nature de l'operation qui s'exerce sur le poste.",
    example = "Tournage",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String nature
) {
  /**
   * Un poste a choisir : sa nature est celle du referentiel, aujourd'hui.
   */
  static RestPosteDeLaJournee from(PosteConnu poste) {
    return de(poste, poste.nature());
  }

  /**
   * Le poste d'une activite, avec la nature figee a sa saisie plutot que celle que le referentiel donne aujourd'hui.
   */
  static RestPosteDeLaJournee de(PosteConnu poste, NatureDOperation nature) {
    return new RestPosteDeLaJournee(poste.id().uuid(), poste.libelle().value(), nature.libelle());
  }
}

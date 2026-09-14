package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.PosteHabilite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
  name = "RestPosteDuPupitre",
  description = """
  Un poste sur lequel l'operateur est habilite, tel que le pupitre le propose au choix.

  Ni nature ni cout horaire : le pupitre offre le poste, il ne le qualifie ni ne le valorise. Proposer cette seule
  liste evite d'avoir a traiter le 409 d'habilitation que le serveur renverrait autrement.
  """
)
record RestPosteDuPupitre(
  @Schema(description = "Identifiant du poste de travail.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Nom du poste tel que l'atelier le designe.", example = "Fraiseuse 1", requiredMode = Schema.RequiredMode.REQUIRED)
  String libelle
) {
  static RestPosteDuPupitre from(PosteHabilite poste) {
    return new RestPosteDuPupitre(poste.id().uuid(), poste.libelle().value());
  }
}

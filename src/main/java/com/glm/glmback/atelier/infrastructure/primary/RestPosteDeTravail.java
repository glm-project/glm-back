package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import java.util.UUID;

@Schema(
  description = """
  Un poste de travail du referentiel, resolu a la lecture.

  Toujours facultatif : une entreprise sans parc machine laisse le champ vide et retrouve une activite unique par
  operateur.
  """
)
record RestPosteDeTravail(
  @Schema(description = "Identifiant du poste dans le referentiel.") UUID id,
  @Schema(description = "Libelle du poste.", example = "Fraiseuse 1") String libelle
) {
  static RestPosteDeTravail resolu(AnnuaireDAtelier annuaire, Optional<PosteDeTravailId> id) {
    return id.flatMap(annuaire::poste).map(RestPosteDeTravail::from).orElse(null);
  }

  private static RestPosteDeTravail from(PosteConnu poste) {
    return new RestPosteDeTravail(poste.id().uuid(), poste.libelle().value());
  }
}

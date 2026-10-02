package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "RestPosteDuCout", description = "Le poste d'un pointage, nomme a la lecture ; libelle absent s'il est inconnu.")
record RestPosteCite(
  @Schema(description = "Identifiant du poste de travail.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Libelle du poste.", example = "DMG DMU 50") String libelle
) {
  static RestPosteCite from(PosteDeTravailId poste, AnnuaireDuCout annuaire) {
    return new RestPosteCite(
      poste.uuid(),
      annuaire
        .poste(poste)
        .map(nomme -> nomme.libelle().value())
        .orElse(null)
    );
  }
}

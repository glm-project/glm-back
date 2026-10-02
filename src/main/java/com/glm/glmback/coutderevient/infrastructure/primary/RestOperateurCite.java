package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.OperateurId;
import com.glm.glmback.coutderevient.domain.OperateurNomme;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import java.util.UUID;

@Schema(
  name = "RestOperateurDuCout",
  description = "L'operateur d'un pointage, nomme a la lecture ; prenom et nom absents s'il est inconnu."
)
record RestOperateurCite(
  @Schema(description = "Identifiant de l'operateur.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Prenom de l'operateur.", example = "Julien") String prenom,
  @Schema(description = "Nom de l'operateur.", example = "Martin") String nom
) {
  static RestOperateurCite from(OperateurId operateur, AnnuaireDuCout annuaire) {
    Optional<OperateurNomme> nomme = annuaire.operateur(operateur);
    return new RestOperateurCite(
      operateur.uuid(),
      nomme.map(connu -> connu.prenom().value()).orElse(null),
      nomme.map(connu -> connu.nom().value()).orElse(null)
    );
  }
}

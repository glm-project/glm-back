package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.domain.OperateurAModifier;
import com.glm.glmback.operateur.domain.OperateurId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Revision d'un operateur.")
record RestModificationOperateur(
  @Schema(description = "Nom de famille.", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 100)
  String nom,

  @Schema(description = "Prenom.", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String prenom,

  @Schema(description = "Matricule interne, laisse vide pour le retirer.", example = "049") @Size(max = 50) String matricule,

  @Schema(description = "Taux horaire de l'operateur, laisse vide pour le retirer.", example = "22.00")
  @DecimalMin(value = "0", inclusive = false)
  BigDecimal tauxHoraire,

  @Schema(description = "Identifiants des postes habilites. La liste fournie remplace la precedente.") Set<UUID> postes
) {
  OperateurAModifier toDomain(OperateurId id) {
    return new OperateurAModifier(id, nom, prenom, matricule, tauxHoraire, RestHabilitations.toDomain(postes));
  }
}

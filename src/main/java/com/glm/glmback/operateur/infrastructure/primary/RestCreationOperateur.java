package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.domain.OperateurACreer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Declaration d'un operateur.")
record RestCreationOperateur(
  @Schema(description = "Nom de famille.", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 100)
  String nom,

  @Schema(description = "Prenom.", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String prenom,

  @Schema(
    description = "Matricule interne. Facultatif : toutes les entreprises n'en attribuent pas. Unique des qu'il est renseigne.",
    example = "049"
  )
  @Size(max = 50)
  String matricule,

  @Schema(
    description = "Taux horaire de l'operateur, destine au cout de revient. Facultatif : toutes les entreprises ne le valorisent pas.",
    example = "22.00"
  )
  @DecimalMin(value = "0", inclusive = false)
  BigDecimal tauxHoraire,

  @Schema(description = "Identifiants des postes sur lesquels l'operateur est habilite. Ce sont eux qui disent ses metiers.")
  Set<UUID> postes
) {
  OperateurACreer toDomain() {
    return new OperateurACreer(nom, prenom, matricule, tauxHoraire, RestHabilitations.toDomain(postes));
  }
}

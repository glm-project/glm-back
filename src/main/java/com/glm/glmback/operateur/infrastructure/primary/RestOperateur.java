package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.domain.Matricule;
import com.glm.glmback.operateur.domain.NatureDeTravail;
import com.glm.glmback.operateur.domain.ProfilDOperateur;
import com.glm.glmback.operateur.domain.TauxHoraire;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Un operateur et les postes sur lesquels il est habilite.")
record RestOperateur(
  @Schema(description = "Identifiant de l'operateur.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,

  @Schema(description = "Nom de famille.", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED) String nom,

  @Schema(description = "Prenom.", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED) String prenom,

  @Schema(description = "Matricule interne, absent si l'entreprise n'en attribue pas.", example = "049") String matricule,

  @Schema(description = "Taux horaire de l'operateur, absent si l'entreprise ne le valorise pas.", example = "22.00")
  BigDecimal tauxHoraire,

  @Schema(description = "Postes habilites, tries par libelle.", requiredMode = Schema.RequiredMode.REQUIRED) List<RestPosteHabilite> postes,

  @Schema(
    description = "Metiers de l'operateur, deduits des natures de ses postes. Jamais saisis.",
    example = "[\"soudage\",\"tournage\"]",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<String> natures
) {
  static RestOperateur from(ProfilDOperateur profil) {
    return new RestOperateur(
      profil.operateur().id().uuid(),
      profil.operateur().nom().value(),
      profil.operateur().prenom().value(),
      profil.operateur().matricule().map(Matricule::value).orElse(null),
      profil.operateur().tauxHoraire().map(TauxHoraire::value).orElse(null),
      profil.postes().stream().map(RestPosteHabilite::from).toList(),
      profil.natures().stream().map(NatureDeTravail::value).toList()
    );
  }
}

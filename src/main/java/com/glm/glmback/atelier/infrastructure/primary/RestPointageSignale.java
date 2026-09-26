package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.MotifDeSignalement;
import com.glm.glmback.atelier.domain.PointageSignale;
import com.glm.glmback.atelier.domain.TypeDeCible;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(
  description = """
  Un pointage enregistre malgre tout, que le gestionnaire doit regarder : son temps compte deja. La ligne sort de la
  liste des qu'il est acquitte, annule ou corrige.
  """
)
record RestPointageSignale(
  @Schema(
    description = "L'evenement signale, qui identifie aussi le signalement pour l'acquitter.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID evenement,
  @Schema(description = "Ou vit l'evenement, pour l'annuler ou le corriger.", requiredMode = Schema.RequiredMode.REQUIRED) RestCible cible,
  @Schema(description = "L'operateur, resolu au referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) RestOperateur operateur,
  @Schema(
    description = """
    OPERATEUR_NON_HABILITE : l'operateur n'etait pas habilite au poste. DATE_ANTERIEURE_A_L_ENGAGEMENT : ramene a
    l'engagement. DATE_FUTURE : ramene a sa reception.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<MotifDeSignalement> motifs,
  @Schema(description = "La date retenue pour l'evenement.", requiredMode = Schema.RequiredMode.REQUIRED) Instant dateDeSurvenue,
  @Schema(description = "La reception par le serveur.", requiredMode = Schema.RequiredMode.REQUIRED) Instant dateDEnregistrement,
  @Schema(description = "La date envoyee par le pupitre, presente seulement quand elle a ete ramenee.") Instant dateDeclaree
) {
  static RestPointageSignale from(PointageSignale pointage, AnnuaireDAtelier annuaire) {
    return new RestPointageSignale(
      pointage.id().uuid(),
      new RestCible(pointage.cible().type(), pointage.cible().id()),
      RestOperateur.resolu(annuaire, pointage.operateur()),
      pointage.motifs().stream().sorted().toList(),
      pointage.horodatage().dateDeSurvenue(),
      pointage.horodatage().dateDEnregistrement(),
      pointage.dateDeclaree().orElse(null)
    );
  }

  @Schema(name = "RestCibleDuSignalement", description = "L'agregat qui porte l'evenement signale.")
  record RestCible(
    @Schema(description = "JOURNEE_DE_TRAVAIL ou SUIVI_D_ATELIER.", requiredMode = Schema.RequiredMode.REQUIRED) TypeDeCible type,
    @Schema(description = "L'identifiant de la journee ou du suivi.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id
  ) {}
}

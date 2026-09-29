package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.application.SynthesesDesHeuresApplicationService;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/syntheses-des-heures")
@Tag(
  name = "Syntheses des heures",
  description = "Le releve horodate des pointages et la duree travaillee d'un operateur, semaine par semaine."
)
class SyntheseDesHeuresResource {

  private static final int PREMIERE_ANNEE = 2000;
  private static final int DERNIERE_ANNEE = 2999;
  private static final int PREMIERE_SEMAINE = 1;
  private static final int DERNIERE_SEMAINE = 53;

  private final SynthesesDesHeuresApplicationService applicationService;

  SyntheseDesHeuresResource(SynthesesDesHeuresApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/{operateurId}")
  @Operation(
    summary = "Lire la synthese des heures hebdomadaire d'un operateur",
    description = """
    Rend les sept jours de la semaine ISO demandee, vides compris, le journal brut de l'operateur et les durees
    de ses activites terminees, y compris automatiquement. Une activite en cours ne produit aucune duree.
    Les activites interpretees par atelier sont selectionnees par recouvrement de la semaine, puis coupees aux
    minuits locaux. Une heure sur deux elements compte sur chacun. Les elements portant une activite ou un pointage
    dans la semaine sont rendus par premiere apparition puis nom, avec leur fiche et leurs postes relus au referentiel.
    Tous les pointages actifs de la semaine sont conserves, meme sans activite interpretable. Leur tri porte sur
    l'heure metier, puis l'intention (fin, transition, ouverture), puis l'identite, jamais l'heure d'enregistrement.
    La semaine est explicite et l'annee est celle des semaines ISO ; aucun montant n'est calcule.
    """
  )
  @ApiResponse(responseCode = "200", description = "La synthese des heures de la semaine demandee.")
  @ApiResponse(responseCode = "400", description = "Annee ou numero de semaine hors bornes.")
  @ApiResponse(responseCode = "404", description = "Operateur inconnu du referentiel.")
  RestSyntheseDesHeures get(
    @Parameter(description = "Identifiant de l'operateur dans le referentiel.") @PathVariable UUID operateurId,
    @Parameter(description = "Annee ISO de la semaine.", example = "2026") @RequestParam @Min(PREMIERE_ANNEE) @Max(
      DERNIERE_ANNEE
    ) int annee,
    @Parameter(description = "Numero de la semaine ISO.", example = "20") @RequestParam @Min(PREMIERE_SEMAINE) @Max(
      DERNIERE_SEMAINE
    ) int semaine
  ) {
    return RestSyntheseDesHeures.from(applicationService.synthese(new OperateurId(operateurId), new SemaineCalendaire(annee, semaine)));
  }
}

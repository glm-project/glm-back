package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.application.FeuillesDeTempsApplicationService;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.SemaineCalendaire;
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
@RequestMapping("/api/feuilles-de-temps")
@Tag(name = "Feuilles de temps", description = "Historique calendaire d'un operateur, semaine par semaine.")
class FeuilleDeTempsResource {

  private static final int PREMIERE_ANNEE = 2000;
  private static final int DERNIERE_ANNEE = 2999;
  private static final int PREMIERE_SEMAINE = 1;
  private static final int DERNIERE_SEMAINE = 53;

  private final FeuillesDeTempsApplicationService applicationService;

  FeuilleDeTempsResource(FeuillesDeTempsApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/{operateurId}")
  @Operation(
    summary = "Lire la feuille de temps hebdomadaire d'un operateur",
    description = """
    Rend les sept jours de la semaine ISO demandee, chacun portant les fenetres de presence qui lui reviennent dans
    le fuseau horaire de l'entreprise.

    La semaine est toujours explicite : aucune semaine courante implicite, pour que deux appels identiques rendent
    toujours la meme chose. L'annee est celle des semaines ISO, qui differe de l'annee civile a ses bornes — la
    semaine 1 de 2026 commence le 29 decembre 2025.
    """
  )
  @ApiResponse(responseCode = "200", description = "La feuille de temps de la semaine demandee.")
  @ApiResponse(responseCode = "400", description = "Annee ou numero de semaine hors bornes.")
  @ApiResponse(responseCode = "404", description = "Operateur inconnu du referentiel.")
  RestFeuilleDeTemps get(
    @Parameter(description = "Identifiant de l'operateur dans le referentiel.") @PathVariable UUID operateurId,
    @Parameter(description = "Annee ISO de la semaine.", example = "2026") @RequestParam @Min(PREMIERE_ANNEE) @Max(
      DERNIERE_ANNEE
    ) int annee,
    @Parameter(description = "Numero de la semaine ISO.", example = "20") @RequestParam @Min(PREMIERE_SEMAINE) @Max(
      DERNIERE_SEMAINE
    ) int semaine
  ) {
    return RestFeuilleDeTemps.from(applicationService.historique(new OperateurId(operateurId), new SemaineCalendaire(annee, semaine)));
  }
}

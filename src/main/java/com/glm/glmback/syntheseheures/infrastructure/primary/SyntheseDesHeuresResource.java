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
    Rend les sept jours de la semaine ISO demandee. Chaque jour porte le journal brut des pointages (arrivee, pause,
    reprise, depart) et la duree travaillee, calculee sur les fenetres de presence, pauses exclues.

    Un pointage qui casse l'automate de presence n'empeche jamais la lecture : il reste visible, marque invalide, et
    n'entre pour rien dans le calcul de la duree. Le jour, puis la semaine, signalent alors une anomalie — le signal
    qu'une correction est attendue sur le journal d'atelier.

    La semaine est toujours explicite : aucune semaine courante implicite, pour que deux appels identiques rendent
    toujours la meme chose. L'annee est celle des semaines ISO, qui differe de l'annee civile a ses bornes — la
    semaine 1 de 2026 commence le 29 decembre 2025.

    Ce releve n'est ni une feuille de paie ni un rapport de paie : il expose du temps travaille pour l'alimenter,
    sans en etre une piece.
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

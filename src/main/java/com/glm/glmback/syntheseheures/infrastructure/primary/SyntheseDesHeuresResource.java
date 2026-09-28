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
    Rend les sept jours de la semaine ISO demandee. Chaque jour porte le journal brut des pointages (arrivee, depart,
    et debut, non conformite ou fin sur un element), la duree de presence, calculee sur les fenetres de presence, et
    le temps operationnel. La pause n'est pas un pointage de presence : la duree de presence la compte.

    Le temps operationnel est rejoue depuis les pointages d'element de l'operateur, poste par poste : un debut sur une
    activite en cours la relance, une non conformite ouvre une reprise, une fin sans activite est ignoree. Une periode
    court jusqu'au pointage suivant sur le meme poste, sinon jusqu'a la cloture du suivi, sinon elle reste ouverte et
    ne compte rien. Elle est reduite aux fenetres de presence de la journee ou elle a commence — un depart la referme,
    une fenetre presumee la rend presumee, un travail commence hors de toute journee ne compte pas — puis coupee a
    minuit. Les durees se cumulent par element : une heure passee sur deux elements compte deux fois. Les evenements
    annules n'apparaissent jamais.

    La semaine rend aussi ses elements, avec leurs durees et les postes sur lesquels ils ont ete travailles.

    La semaine est toujours explicite : aucune semaine courante implicite. L'annee est celle des semaines ISO, qui
    differe de l'annee civile a ses bornes — la semaine 1 de 2026 commence le 29 decembre 2025.

    Une journee sans depart au-dela de l'amplitude maximale de l'entreprise est abandonnee : elle compte jusqu'a son
    dernier fait connu, dans dureePresumee et jamais dans duree. C'est l'instant de lecture qui en decide, donc deux
    appels espaces peuvent differer.

    Ce releve n'est ni une feuille de paie ni un rapport de paie : il releve la presence, qui ne sert pas a payer.
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

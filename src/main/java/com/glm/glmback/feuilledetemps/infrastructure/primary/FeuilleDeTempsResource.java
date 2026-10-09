package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.application.FeuillesDeTempsApplicationService;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.SemaineCalendaire;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    Rend les sept jours de la semaine ISO demandee, chacun portant les portions d'activite qui lui reviennent
    dans le fuseau horaire de l'entreprise.

    Le travail est lu dans la projection des activites interpretees par atelier, selectionnee par recouvrement :
    une activite commencee avant la semaine reste visible. Chaque portion
    porte l'identite stable, l'etat et les bornes entieres de l'activite, en plus de ses bornes coupees a minuit et a
    la semaine. Une fin reelle est conservee ; sans elle, l'activite est terminee automatiquement a son echeance des
    qu'elle est atteinte. Cet etat signale l'anomalie. Une activite en cours n'a pas de fin.
    Une activite en cours rend une indication sans fin sur chaque jour atteint a l'instant de lecture, dans la
    semaine. Son debut entier permet de lire « en cours depuis dimanche » sur lundi, sans fin fabriquee a minuit.

    La semaine est toujours explicite : aucune semaine courante implicite. L'annee est celle des semaines ISO, qui
    differe de l'annee civile a ses bornes — la semaine 1 de 2026 commence le 29 decembre 2025.

    L'instant evaluation facultatif decide de l'expiration et des jours atteints par les activites en cours.
    Sans parametre, l'heure du serveur est relevee une seule fois. La reponse rend l'instant effectivement utilise.
    Un instant passe est accepte ; la limite future est l'heure du serveur plus deux minutes, incluse.
    Au-dela, ou si l'instant fourni est vide ou mal forme, la lecture est refusee en 400, sans rapport.
    Passer le meme instant a la feuille et a la synthese assure la meme decision d'expiration ; les faits connus
    restent lus, meme posterieurs a cet instant. Ce contrat ne garantit ni lecture historique ni transaction commune.
    """
  )
  @ApiResponse(responseCode = "200", description = "La feuille de temps de la semaine demandee.")
  @ApiResponse(
    responseCode = "400",
    description = "Annee ou numero de semaine hors bornes, ou instant evaluation vide, mal forme ou trop futur."
  )
  @ApiResponse(responseCode = "404", description = "Operateur inconnu du referentiel.")
  RestFeuilleDeTemps get(
    @Parameter(description = "Identifiant de l'operateur dans le referentiel.") @PathVariable UUID operateurId,
    @Parameter(description = "Annee ISO de la semaine.", example = "2026") @RequestParam @Min(PREMIERE_ANNEE) @Max(
      DERNIERE_ANNEE
    ) int annee,
    @Parameter(description = "Numero de la semaine ISO.", example = "20") @RequestParam @Min(PREMIERE_SEMAINE) @Max(
      DERNIERE_SEMAINE
    ) int semaine,
    @Parameter(
      description = "Instant ISO-8601 utilise pour evaluer les activites. Par defaut, heure du serveur. Accepte jusqu'a serveur plus deux minutes, borne incluse.",
      schema = @Schema(type = "string", format = "date-time")
    ) @RequestParam(required = false) String evaluation
  ) {
    return RestFeuilleDeTemps.from(
      applicationService.historique(new OperateurId(operateurId), new SemaineCalendaire(annee, semaine), instantDEvaluation(evaluation))
    );
  }

  private static Optional<Instant> instantDEvaluation(String evaluation) {
    try {
      return Optional.ofNullable(evaluation).map(Instant::parse);
    } catch (DateTimeParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'instant evaluation doit respecter le format ISO-8601.", e);
    }
  }
}

package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.application.SynthesesDesHeuresApplicationService;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
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
  name = "Rapports d'activite",
  description = "Toute l'activite d'un operateur sur une semaine ou un mois, a telecharger. Du temps pointe, aucun montant."
)
class RapportDActiviteResource {

  private static final int PREMIERE_ANNEE = 2000;
  private static final int DERNIERE_ANNEE = 2999;
  private static final int PREMIERE_SEMAINE = 1;
  private static final int DERNIERE_SEMAINE = 53;
  private static final int PREMIER_MOIS = 1;
  private static final int DERNIER_MOIS = 12;
  private static final String DESCRIPTION_DE_LA_PERIODE = """
    La periode est une semaine ISO (annee ISO et semaine) ou un mois civil (annee et mois), au fuseau de
    l'entreprise ; exactement l'un des deux. Les activites sont coupees aux minuits locaux et aux bornes de la
    periode : une semaine a cheval sur deux mois est coupee dans le rapport mensuel. Une periode qui n'a pas encore
    commence est refusee.
    """;

  private final SynthesesDesHeuresApplicationService applicationService;

  RapportDActiviteResource(SynthesesDesHeuresApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/{operateurId}/rapport")
  @Operation(
    summary = "Lire le resume du rapport d'activite d'un operateur",
    description = """
      Ce qu'affiche la fenetre d'export avant le telechargement : temps pointe, jours avec pointage, nombre de pointages
      et de fins automatiques. Memes regles que la synthese des heures : une heure passee sur deux elements compte sur
      chacun, une fin automatique compte jusqu'a son echeance, une activite en cours ne compte rien.

      """
      + DESCRIPTION_DE_LA_PERIODE
  )
  @ApiResponse(responseCode = "200", description = "Le resume du rapport de la periode.")
  @ApiResponse(
    responseCode = "400",
    description = "Annee, semaine ou mois hors bornes, semaine et mois tous deux absents ou tous deux presents, ou periode future."
  )
  @ApiResponse(responseCode = "403", description = "Jeton sans entreprise connue, ou role autre que GESTIONNAIRE.")
  @ApiResponse(responseCode = "404", description = "Operateur inconnu du referentiel.")
  RestResumeDuRapportDActivite resume(
    @Parameter(description = "Identifiant de l'operateur dans le referentiel.") @PathVariable UUID operateurId,
    @Parameter(description = "Annee : ISO pour une semaine, civile pour un mois.", example = "2026") @RequestParam @Min(
      PREMIERE_ANNEE
    ) @Max(DERNIERE_ANNEE) int annee,
    @Parameter(description = "Numero de la semaine ISO.", example = "38") @RequestParam(required = false) @Min(PREMIERE_SEMAINE) @Max(
      DERNIERE_SEMAINE
    ) Integer semaine,
    @Parameter(description = "Numero du mois, de 1 a 12.", example = "9") @RequestParam(required = false) @Min(PREMIER_MOIS) @Max(
      DERNIER_MOIS
    ) Integer mois
  ) {
    return RestResumeDuRapportDActivite.from(
      applicationService.rapport(
        new OperateurId(operateurId),
        PeriodeDemandee.de(annee, Optional.ofNullable(semaine), Optional.ofNullable(mois))
      )
    );
  }
}

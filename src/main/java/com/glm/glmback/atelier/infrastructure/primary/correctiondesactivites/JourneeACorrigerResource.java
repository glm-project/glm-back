package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.application.correctiondesactivites.JourneeACorrigerApplicationService;
import com.glm.glmback.atelier.domain.OperateurId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/operateurs")
@Tag(name = "Atelier - correction des activites")
class JourneeACorrigerResource {

  private final JourneeACorrigerApplicationService applicationService;

  JourneeACorrigerResource(JourneeACorrigerApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/{operateurId}/journees/{jour}")
  @Operation(
    summary = "Lire la journee d'un operateur a corriger",
    description = """
    La journee d'un operateur telle que le gestionnaire la corrige : les activites qui touchent le jour, apres les retraits
    et les changements de poste deja faits, avec leur suivi (une feuille de temps ne donne que l'element, jamais le suivi
    qu'une correction vise), leur etat et leurs portions coupees aux minuits de l'entreprise ; la
    duree operationnelle du jour ; les minuits du jour, du lendemain compris ; et les choix d'un ajout, les suivis presents
    ce jour-la et les postes habilites de l'operateur.

    Le jour est un jour calendaire `AAAA-MM-JJ` du fuseau de l'entreprise (aujourd'hui Europe/Paris), les instants se lisent
    en UTC comme dans le reste de l'API (`evaluation` accepte un decalage) et les durees suivent ISO-8601. `evaluation`, facultatif comme dans les autres lectures, fixe l'instant
    auquel une activite sans fin est jugee en cours ou terminee automatiquement ; par defaut, l'heure du serveur.
    """
  )
  @ApiResponse(responseCode = "200", description = "La journee de l'operateur.")
  @ApiResponse(responseCode = "400", description = "Le jour ou l'instant d'evaluation est mal forme.", content = @Content)
  @ApiResponse(responseCode = "404", description = "Operateur inconnu du referentiel (`operateur-introuvable`).", content = @Content)
  RestJourneeACorriger get(
    @Parameter(description = "Identifiant de l'operateur dans le referentiel.") @PathVariable UUID operateurId,
    @Parameter(description = "Le jour calendaire, AAAA-MM-JJ.", example = "2026-10-05") @PathVariable @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate jour,
    @Parameter(
      description = "Instant ISO-8601 utilise pour evaluer les activites sans fin. Par defaut, l'heure du serveur.",
      schema = @Schema(type = "string", format = "date-time")
    ) @RequestParam(required = false) Instant evaluation
  ) {
    return RestJourneeACorriger.from(applicationService.lit(new OperateurId(operateurId), jour, Optional.ofNullable(evaluation)));
  }
}

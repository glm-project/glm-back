package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.application.SynthesesDesHeuresApplicationService;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.RapportDActivite;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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

  @GetMapping(path = "/{operateurId}/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @Operation(
    summary = "Exporter le rapport d'activite d'un operateur en classeur Excel",
    description = """
      Le meme rapport que le resume, mis en forme pour etre retravaille. Une fin automatique ne bloque pas l'export :
      elle compte jusqu'a son echeance et porte la mention "fin automatique".

      L'onglet Synthese porte l'operateur, la periode, l'instant de generation, le temps pointe et les tableaux nommes
      Jours (un mois omet ses samedis et dimanches sans pointage), Elements et Natures (seulement s'il y a au moins deux
      natures). L'onglet Pointages est un tableau Excel nomme Pointages, une ligne par pointage (date, jour, debut, fin,
      duree, element, reference, description, categorie, poste, nature, activite, mention), sans cellule fusionnee ni
      ligne intercalaire : additionnees, ses durees donnent le temps pointe. Durees en heures decimales, jours et heures
      en vraies dates Excel dans le fuseau de l'entreprise ; une duree nulle garde sa valeur sous un format qui
      l'affiche vide.

      """
      + DESCRIPTION_DE_LA_PERIODE
  )
  @ApiResponse(
    responseCode = "200",
    description = "Le classeur, en piece jointe nommee d'apres l'operateur et la periode (activite-dupont-jean-2026-05.xlsx).",
    content = @Content(
      mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      schema = @Schema(type = "string", format = "binary")
    )
  )
  @ApiResponse(
    responseCode = "400",
    description = "Annee, semaine ou mois hors bornes, semaine et mois tous deux absents ou tous deux presents, ou periode future."
  )
  @ApiResponse(responseCode = "403", description = "Jeton sans entreprise connue, ou role autre que GESTIONNAIRE.")
  @ApiResponse(responseCode = "404", description = "Operateur inconnu du referentiel.")
  ResponseEntity<byte[]> exporteEnExcel(
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
  ) throws IOException {
    RapportDActivite rapport = applicationService.rapport(
      new OperateurId(operateurId),
      PeriodeDemandee.de(annee, Optional.ofNullable(semaine), Optional.ofNullable(mois))
    );

    return ResponseEntity.ok()
      .contentType(ClasseurDuRapportDActivite.TYPE)
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment().filename(ClasseurDuRapportDActivite.nomDeFichier(rapport)).build().toString()
      )
      .body(ClasseurDuRapportDActivite.de(rapport));
  }
}

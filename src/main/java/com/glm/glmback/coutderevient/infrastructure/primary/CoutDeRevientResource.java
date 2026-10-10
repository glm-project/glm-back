package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.application.CoutsDeRevientApplicationService;
import com.glm.glmback.coutderevient.domain.CompteRenduDuCout;
import com.glm.glmback.coutderevient.domain.ElementId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/couts-de-revient")
@Tag(name = "Couts de revient", description = "Le temps passe sur un element de fabrication, valorise par nature d'operation.")
class CoutDeRevientResource {

  private final CoutsDeRevientApplicationService applicationService;

  CoutDeRevientResource(CoutsDeRevientApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/{elementId}")
  @Operation(
    summary = "Lire le cout de revient d'un element de fabrication",
    description = """
    Rend une ligne par nature d'operation — fraisage, tournage, erosion —, chacune portant le temps de bon travail,
    le temps passe en non conformite avec ses periodes datees, et le cout separe en machine et main d'oeuvre.

    L'entree se fait par l'element, non par son suivi d'atelier : un element reengage apres cloture additionne ses
    passages.

    Deux regles a connaitre avant d'afficher les montants. Le cout horaire de chaque poste actif court **en entier**,
    meme quand l'operateur en mene plusieurs de front ; son taux horaire, lui, est **divise** par le nombre de postes
    qu'il occupait a cet instant, tous elements confondus. Un operateur sur trois elements avec une seule machine
    n'est donc pas divise. Le decoupage se fait aux bornes de chaque pointage, si bien que seul le chevauchement reel
    est divise.

    Une activite encore en cours est entierement exclue du temps, des couts et du diviseur. A son echeance
    projetee, elle compte jusqu'a sa fin automatique avec une anomalie et sa periode datee. L'horloge est relevee
    une seule fois par rapport ; cet instant est rendu dans evaluation.

    Chaque duree et montant porte sa valeur.
    """
  )
  @ApiResponse(responseCode = "200", description = "Le rapport de l'element. Vide s'il n'a jamais ete engage en atelier.")
  @ApiResponse(responseCode = "403", description = "Jeton sans entreprise connue, ou role autre que GESTIONNAIRE.")
  @ApiResponse(responseCode = "404", description = "Element de fabrication inconnu.")
  RestCoutDeRevient get(@Parameter(description = "Identifiant de l'element de fabrication.") @PathVariable UUID elementId) {
    return RestCoutDeRevient.from(applicationService.rapport(new ElementId(elementId)));
  }

  @GetMapping(path = "/{elementId}/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @Operation(
    summary = "Exporter le cout de revient d'un element en classeur Excel",
    description = """
    Le meme rapport que la lecture, mis en forme pour le client : aucun montant n'est recalcule ni arrondi a nouveau.

    L'onglet Synthese porte l'element, son statut, l'instant de generation (l'evaluation du rapport) et une ligne par
    nature avec le total. L'element est "Termine le" sa derniere cloture quand tous ses passages en atelier sont clos
    et qu'aucune activite n'est en cours ; sinon il est "En cours", et le classeur est une photographie a l'instant de
    generation. Montants et durees sont des nombres (durees en heures decimales), les dates de vraies dates Excel
    dans le fuseau de l'entreprise ; un cout de 0 EUR garde sa valeur sous un format qui l'affiche vide.
    """
  )
  @ApiResponse(
    responseCode = "200",
    description = "Le classeur, en piece jointe nommee d'apres l'element (cout-de-revient-OF-2026-000001.xlsx).",
    content = @Content(
      mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      schema = @Schema(type = "string", format = "binary")
    )
  )
  @ApiResponse(responseCode = "403", description = "Jeton sans entreprise connue, ou role autre que GESTIONNAIRE.")
  @ApiResponse(responseCode = "404", description = "Element de fabrication inconnu.")
  ResponseEntity<byte[]> exporteEnExcel(@Parameter(description = "Identifiant de l'element de fabrication.") @PathVariable UUID elementId)
    throws IOException {
    CompteRenduDuCout compteRendu = applicationService.compteRendu(new ElementId(elementId));

    return ResponseEntity.ok()
      .contentType(ClasseurDuCoutDeRevient.TYPE)
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment().filename(ClasseurDuCoutDeRevient.nomDeFichier(compteRendu)).build().toString()
      )
      .body(ClasseurDuCoutDeRevient.de(compteRendu));
  }
}

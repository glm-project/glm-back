package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.gestionanomalies.ListeDesAnomaliesApplicationService;
import com.glm.glmback.atelier.domain.gestionanomalies.AnomaliesDAtelierCriteria;
import com.glm.glmback.atelier.domain.gestionanomalies.NatureDAnomalie;
import com.glm.glmback.shared.pagination.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/anomalies")
@Tag(
  name = "Atelier - anomalies de pointage",
  description = "Ce que le gestionnaire doit trancher : une anomalie de pointage porte une nature. Deux natures : CONFLIT, une sequence en conflit, et FIN_AUTOMATIQUE, une activite terminee a son echeance faute de fin reelle. Le parcours est le meme pour chaque dossier : consulter, previsualiser, confirmer."
)
class ListeDesAnomaliesResource {

  private final ListeDesAnomaliesApplicationService anomalies;

  ListeDesAnomaliesResource(ListeDesAnomaliesApplicationService anomalies) {
    this.anomalies = anomalies;
  }

  @GetMapping
  @Operation(
    summary = "Lister les anomalies de pointage d'une nature",
    description = """
    nature est obligatoire : absente ou inconnue, elle est refusee en 400 avec le code
    urn:glm:erreur:atelier:nature-d-anomalie-invalide. La reponse est discriminee par nature : chaque ligne porte
    nature, CONFLIT ou FIN_AUTOMATIQUE, celle demandee.
    CONFLIT liste les sequences en conflit, une ligne par sequence, y compris pour un suivi cloture ou sans activite
    deduite. Le tri porte sur le premier pointage metier, puis le suivi et l'ancrage de la sequence.
    FIN_AUTOMATIQUE liste les activites sans fin reelle, hors activites a resoudre, dont l'echeance est atteinte a
    l'instant de la lecture, borne comprise. Chaque ligne porte l'adresse du dossier (suivi et pointage ouvrant actif),
    l'activite d'origine, son debut et son echeance, sans duree. Le tri porte sur le debut, puis le suivi et l'ouvrant.
    Les recherches partielles operateur (nom, prenom ou identifiant) et element (designation ou identifiant)
    sont combinees sans tenir compte de la casse avant pagination. Pourcent, soulignement et antislash sont litteraux.
    complete vaut true lorsque la lecture a reussi, meme si aucune ligne ne correspond. Une acquisition en echec
    reste une erreur HTTP. Les diagnostics detailles se consultent dans le dossier de l'anomalie.
    """
  )
  @ApiResponse(responseCode = "200", description = "Une page d'anomalies de la nature demandee, pour USER ou GESTIONNAIRE.")
  @ApiResponse(responseCode = "400", description = "Nature absente ou inconnue : urn:glm:erreur:atelier:nature-d-anomalie-invalide.")
  RestPageDesAnomalies list(
    @Parameter(required = true, schema = @Schema(implementation = NatureDAnomalie.class)) @RequestParam(required = false) String nature,
    @RequestParam(defaultValue = "") String operateur,
    @RequestParam(defaultValue = "") String element,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return switch (NatureDAnomalie.of(nature)) {
      case CONFLIT -> RestPageDesAnomalies.from(
        anomalies.listConflits(new AnomaliesDAtelierCriteria(operateur, element), new Pageable(page, size))
      );
      case FIN_AUTOMATIQUE -> RestPageDesAnomalies.from(
        anomalies.listFinsAutomatiques(new AnomaliesDAtelierCriteria(operateur, element), new Pageable(page, size))
      );
    };
  }
}

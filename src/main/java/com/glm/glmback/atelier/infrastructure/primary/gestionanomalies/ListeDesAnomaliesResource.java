package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.gestionanomalies.ListeDesAnomaliesApplicationService;
import com.glm.glmback.atelier.domain.gestionanomalies.AnomaliesDAtelierCriteria;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
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
  description = "Ce que le gestionnaire doit trancher : une activite terminee a son echeance faute de fin reelle."
)
class ListeDesAnomaliesResource {

  private final ListeDesAnomaliesApplicationService anomalies;

  ListeDesAnomaliesResource(ListeDesAnomaliesApplicationService anomalies) {
    this.anomalies = anomalies;
  }

  @GetMapping
  @Operation(
    summary = "Lister les fins automatiques",
    description = """
    Les activites sans fin reelle dont l'echeance est atteinte a l'instant de la lecture, borne comprise. Chaque ligne
    porte l'adresse du dossier (suivi et pointage ouvrant), l'activite, son debut et son echeance, sans duree. Le tri
    porte sur le debut, puis le suivi et l'ouvrant.
    Les recherches partielles operateur (nom, prenom ou identifiant) et element (designation ou identifiant)
    sont combinees sans tenir compte de la casse avant pagination. Pourcent, soulignement et antislash sont litteraux.
    Une acquisition en echec reste une erreur HTTP.
    """
  )
  @ApiResponse(responseCode = "200", description = "Une page de fins automatiques, pour USER ou GESTIONNAIRE.")
  RestPage<RestFinAutomatiqueEnListe> list(
    @RequestParam(defaultValue = "") String operateur,
    @RequestParam(defaultValue = "") String element,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    var lecture = anomalies.listFinsAutomatiques(new AnomaliesDAtelierCriteria(operateur, element), new Pageable(page, size));
    return RestPage.from(lecture.page(), ligne -> RestFinAutomatiqueEnListe.from(ligne, lecture.annuaire()));
  }
}

package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDossierAnomalie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/suivis")
@Tag(name = "Atelier - anomalies de pointage")
class DossierAnomalieResource {

  private final SuivisDAtelierApplicationService applicationService;

  DossierAnomalieResource(SuivisDAtelierApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/{id}/anomalies/{pointage}")
  @Operation(
    summary = "Consulter le dossier d'une fin automatique",
    description = "Le dossier de l'activite echue que le pointage ouvre : l'activite et les pointages de sa cle, a l'instant d'evaluation. Une fin automatique deja regularisee n'a plus de dossier."
  )
  @ApiResponse(responseCode = "200", description = "Le dossier de la fin automatique non regularisee, pour USER ou GESTIONNAIRE.")
  @ApiResponse(
    responseCode = "404",
    description = "Suivi introuvable dans l'entreprise courante, ou pointage qui n'ouvre aucune fin automatique non regularisee."
  )
  RestDossierAnomalie dossier(@PathVariable UUID id, @PathVariable UUID pointage) {
    var lecture = applicationService.get(new SuiviDAtelierId(id));
    var dossier = LectureDossierAnomalie.de(new AdresseDossierAnomalie(lecture.suivi().id(), new EvenementDAtelierId(pointage)), lecture);
    return RestDossierAnomalie.from(dossier, applicationService.annuairePour(lecture.suivi()));
  }
}

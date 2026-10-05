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
@Tag(name = "Atelier - elements engages")
class DossierAnomalieResource {

  private final SuivisDAtelierApplicationService applicationService;

  DossierAnomalieResource(SuivisDAtelierApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/{id}/conflits/{pointage}")
  @Operation(
    summary = "Consulter un dossier de conflit par un pointage",
    description = "Le journal, la revision et les consequences interpretees du suivi a un instant d'evaluation. L'adresse ne redirige jamais vers une autre sequence : une ancre absente, annulee ou active hors conflit garde son etat et l'historique accessible."
  )
  @ApiResponse(responseCode = "200", description = "Le dossier et l'etat de son adresse, pour USER ou GESTIONNAIRE.")
  @ApiResponse(responseCode = "404", description = "Suivi introuvable dans l'entreprise courante.")
  RestDossierConflit dossier(@PathVariable UUID id, @PathVariable UUID pointage) {
    var lecture = applicationService.get(new SuiviDAtelierId(id));
    var dossier = new LectureDossierAnomalie(new AdresseDossierAnomalie(lecture.suivi().id(), new EvenementDAtelierId(pointage)), lecture);
    return RestDossierConflit.from(dossier, applicationService.annuairePour(lecture.suivi()));
  }
}

package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.SupervisionDAtelierApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/supervision")
@Tag(name = "Atelier - supervision")
class SupervisionDAtelierResource {

  private final SupervisionDAtelierApplicationService supervision;

  SupervisionDAtelierResource(SupervisionDAtelierApplicationService supervision) {
    this.supervision = supervision;
  }

  @GetMapping
  @Operation(summary = "Lire la supervision complete de l'entreprise connectee")
  RestSupervisionDAtelier read() {
    var lecture = supervision.read();
    return new RestSupervisionDAtelier(
      lecture.evaluation(),
      lecture.operateurs().stream().map(RestOperateurDeSupervision::from).toList(),
      lecture.activites().stream().map(RestActiviteDeSupervision::from).toList(),
      lecture.sequencesEnConflit().stream().map(RestSequenceEnConflitDeSupervision::from).toList()
    );
  }
}

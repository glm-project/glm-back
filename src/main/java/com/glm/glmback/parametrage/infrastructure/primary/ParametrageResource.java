package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.application.ParametrageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parametrage")
@Tag(
  name = "Parametrage",
  description = """
  Les reglages que l'entreprise fixe elle-meme, un seul jeu pour toute l'entreprise.

  Le gestionnaire et l'operateur (role USER) les consultent.
  """
)
class ParametrageResource {

  private final ParametrageApplicationService applicationService;

  ParametrageResource(ParametrageApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(summary = "Lire le parametrage de l'entreprise", description = "Un reglage jamais fixe vaut sa valeur par defaut.")
  @ApiResponse(responseCode = "200", description = "Le parametrage de l'entreprise.")
  RestParametrage lisLeParametrage() {
    return RestParametrage.from(applicationService.get());
  }
}

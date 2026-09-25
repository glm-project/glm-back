package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.application.ParametrageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parametrage")
@Tag(
  name = "Parametrage",
  description = """
  Le parametrage de l'entreprise, seme a sa creation avec des valeurs par defaut.

  Le gestionnaire le modifie ; l'operateur (role USER) le consulte. Seule la derniere modification est tracee.
  """
)
class ParametrageResource {

  private final ParametrageApplicationService applicationService;

  ParametrageResource(ParametrageApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(summary = "Consulter le parametrage de l'entreprise")
  @ApiResponse(responseCode = "200", description = "Le parametrage, avec sa derniere modification s'il en a eu une.")
  RestParametrage get() {
    return RestParametrage.from(applicationService.get());
  }

  @PutMapping("/amplitude-maximale")
  @Operation(
    summary = "Fixer l'amplitude maximale d'une journee de travail",
    description = """
    Au-dela de cette duree depuis l'arrivee, pauses comprises, une journee sans depart est abandonnee. 13 h par
    defaut. La valeur se compte a la minute et reste strictement sous 24 h, pour qu'un retour le lendemain a la meme
    heure soit toujours une nouvelle arrivee.
    """
  )
  @ApiResponse(responseCode = "200", description = "Le parametrage modifie.")
  @ApiResponse(responseCode = "400", description = "Valeur absente, illisible, hors de ]0, 24 h[ ou plus fine que la minute.")
  RestParametrage fixeLAmplitudeMaximale(@RequestBody @Valid RestAmplitudeMaximale request) {
    return RestParametrage.from(applicationService.fixeLAmplitudeMaximale(request.toDomain(), AuteurConnecte.get()));
  }
}

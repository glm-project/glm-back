package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.AnomaliesApplicationService;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnomalieDePresence;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.TypeDAnomalie;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/anomalies")
@Tag(
  name = "Atelier - anomalies de presence",
  description = """
  Ce que le gestionnaire doit regarder : les journees abandonnees sans depart, et les journees fermees au-dela de
  l'amplitude maximale de l'entreprise. Jugees a l'instant de lecture avec le seuil courant, jamais stockees.
  """
)
class AnomalieDePresenceResource {

  private final AnomaliesApplicationService applicationService;

  AnomalieDePresenceResource(AnomaliesApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les anomalies de presence",
    description = """
    La plus recente d'abord, par arrivee. Filtres facultatifs sur l'operateur et le type. Une ligne disparait des que
    la regularisation du depart la resout ; une amplitude excessive reste tant que la journee n'est pas corrigee.
    """
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, triee par arrivee descendante.")
  @ApiResponse(responseCode = "400", description = "Type d'anomalie inconnu.")
  RestPage<RestAnomalieDePresence> list(
    @RequestParam(required = false) UUID operateur,
    @RequestParam(required = false) TypeDAnomalie type,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<AnomalieDePresence> resultat = applicationService.list(
      Optional.ofNullable(operateur).map(OperateurId::new),
      Optional.ofNullable(type),
      new Pageable(page, size)
    );
    AnnuaireDAtelier annuaire = applicationService.annuairePour(resultat.content());

    return RestPage.from(resultat, anomalie -> RestAnomalieDePresence.from(anomalie, annuaire));
  }
}

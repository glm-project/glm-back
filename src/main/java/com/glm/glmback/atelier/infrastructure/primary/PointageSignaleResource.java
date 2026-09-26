package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.PointagesSignalesApplicationService;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.MotifDeSignalement;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageSignale;
import com.glm.glmback.atelier.domain.PointageSignaleId;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/pointages-signales")
@Tag(
  name = "Atelier - pointages signales",
  description = """
  Les pointages du pupitre enregistres malgre tout : operateur non habilite au poste, date anterieure a l'engagement
  ou future. Leur temps compte deja ; le gestionnaire les regarde, puis les acquitte, les annule ou les corrige.
  L'operateur n'en est pas informe.
  """
)
class PointageSignaleResource {

  private final PointagesSignalesApplicationService applicationService;

  PointageSignaleResource(PointagesSignalesApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les pointages signales",
    description = """
    Les signalements non resolus, le plus recent d'abord par date retenue. Filtres facultatifs sur l'operateur et le
    motif.
    """
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, triee par date retenue descendante.")
  @ApiResponse(responseCode = "400", description = "Motif de signalement inconnu.")
  RestPage<RestPointageSignale> list(
    @RequestParam(required = false) UUID operateur,
    @RequestParam(required = false) MotifDeSignalement motif,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<PointageSignale> resultat = applicationService.list(
      Optional.ofNullable(operateur).map(OperateurId::new),
      Optional.ofNullable(motif),
      new Pageable(page, size)
    );
    AnnuaireDAtelier annuaire = applicationService.annuairePour(resultat.content());

    return RestPage.from(resultat, pointage -> RestPointageSignale.from(pointage, annuaire));
  }

  @PostMapping("/{evenement}/acquittement")
  @Operation(
    summary = "Acquitter un pointage signale",
    description = "Le gestionnaire juge le pointage legitime : son temps compte toujours, la ligne sort de la liste."
  )
  @ApiResponse(responseCode = "200", description = "Le signalement acquitte.")
  @ApiResponse(responseCode = "404", description = "Aucun pointage signale ne porte cet evenement.")
  @ApiResponse(responseCode = "409", description = "Le signalement est deja acquitte, annule ou corrige.")
  RestPointageSignale acquitte(@PathVariable UUID evenement) {
    PointageSignale acquitte = applicationService.acquitte(new PointageSignaleId(evenement), AuteurConnecte.get());

    return RestPointageSignale.from(acquitte, applicationService.annuairePour(List.of(acquitte)));
  }
}

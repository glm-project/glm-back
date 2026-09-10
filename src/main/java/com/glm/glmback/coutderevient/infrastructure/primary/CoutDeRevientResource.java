package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.application.CoutsDeRevientApplicationService;
import com.glm.glmback.coutderevient.domain.ElementId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
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

    Un travail encore en cours est arrete a l'instant de la lecture : deux appels espaces ne rendent donc pas la meme
    chose sur un element en cours, ce qui est le seul moyen de chiffrer ce qui n'est pas termine.
    """
  )
  @ApiResponse(responseCode = "200", description = "Le rapport de l'element. Vide s'il n'a jamais ete engage en atelier.")
  @ApiResponse(responseCode = "403", description = "Jeton sans entreprise connue, ou role autre que GESTIONNAIRE.")
  @ApiResponse(responseCode = "404", description = "Element de fabrication inconnu.")
  RestCoutDeRevient get(@Parameter(description = "Identifiant de l'element de fabrication.") @PathVariable UUID elementId) {
    return RestCoutDeRevient.from(applicationService.rapport(new ElementId(elementId)));
  }
}

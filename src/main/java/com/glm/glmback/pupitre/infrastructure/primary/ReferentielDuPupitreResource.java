package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.application.ReferentielsDuPupitreApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pupitre")
@Tag(name = "Pupitre", description = "Ce que le poste d'atelier met en cache pour continuer a collecter sans reseau.")
class ReferentielDuPupitreResource {

  private final ReferentielsDuPupitreApplicationService applicationService;

  ReferentielDuPupitreResource(ReferentielsDuPupitreApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping("/referentiel")
  @Operation(
    summary = "Lire tout le referentiel du pupitre en un appel",
    description = """
    Rend en une seule reponse les operateurs designables avec leurs habilitations et leur etat de presence, et les
    elements sur lesquels on peut encore pointer avec leurs activites en cours. C'est la lecture que le pupitre
    rejoue a chaque synchronisation pour rafraichir son cache local.

    Volontairement non paginee : la pagination est exactement ce qui empeche de prouver une version instantanee du
    referentiel, puisque rien ne garantit que deux pages viennent du meme etat de la base. Tout est ici lu dans une
    transaction unique.

    `etat` dit quelles commandes de presence l'ecran peut offrir, y compris hors ligne : c'est l'etat de la journee
    en cours de l'operateur, sans borne de date. Un operateur sans journee en cours vaut ABSENT et reste rendu, la
    liste etant celle des operateurs designables et non des operateurs presents.

    `genereLe` est la version de cet instantane. Elle change a chaque appel, y compris quand rien n'a bouge : elle
    dit quand le serveur a produit la reponse, pas quand le referentiel a change pour la derniere fois.

    Ce que la reponse ne porte pas, et n'a pas a porter : aucun montant — ni taux horaire d'operateur, ni cout
    horaire de poste —, aucun journal d'evenements, aucun element cloture, et aucun instant de presence.
    """
  )
  @ApiResponse(responseCode = "200", description = "L'instantane du referentiel, date de l'instant de sa lecture.")
  RestReferentielDuPupitre referentiel() {
    return RestReferentielDuPupitre.from(applicationService.referentiel());
  }
}

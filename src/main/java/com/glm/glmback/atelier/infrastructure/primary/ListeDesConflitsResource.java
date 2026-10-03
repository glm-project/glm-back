package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.ListeDesConflitsApplicationService;
import com.glm.glmback.atelier.domain.ConflitsDAtelierCriteria;
import com.glm.glmback.shared.pagination.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/conflits")
@Tag(name = "Atelier - resolution des conflits")
class ListeDesConflitsResource {

  private final ListeDesConflitsApplicationService conflits;

  ListeDesConflitsResource(ListeDesConflitsApplicationService conflits) {
    this.conflits = conflits;
  }

  @GetMapping
  @Operation(
    summary = "Lister les sequences en conflit",
    description = """
    Une ligne par sequence, y compris pour un suivi cloture ou sans activite deduite.
    Les recherches partielles operateur (nom, prenom ou identifiant) et element (designation ou identifiant)
    sont combinees sans tenir compte de la casse avant pagination. Pourcent, soulignement et antislash sont litteraux.
    Le tri porte sur le premier pointage metier, puis le suivi et l'ancrage de la sequence.
    complete vaut true lorsque la lecture a reussi, meme si aucune sequence ne correspond. Une acquisition en echec
    reste une erreur HTTP. Les diagnostics detailles se consultent dans le dossier de la sequence.
    """
  )
  RestPageDesConflits list(
    @RequestParam(defaultValue = "") String operateur,
    @RequestParam(defaultValue = "") String element,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return RestPageDesConflits.from(conflits.list(new ConflitsDAtelierCriteria(operateur, element), new Pageable(page, size)));
  }
}

package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.application.PostesDeTravailApplicationService;
import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postes-de-travail")
@Tag(
  name = "Postes de travail",
  description = """
  Le referentiel de ce sur quoi les operateurs pointent : une machine, un etabli, un four, une salle.

  Chaque poste porte la nature du travail qui s'y fait. C'est de la que vient le metier exerce a un instant donne :
  l'operateur qui demarre sur le poste de soudure soude, celui qui demarre sur un tour tourne. Rien de tout cela n'est
  declare sur la personne.

  Le gestionnaire declare et revise les postes ; l'operateur (role USER) les consulte.
  """
)
class PosteDeTravailResource {

  private final PostesDeTravailApplicationService applicationService;

  PosteDeTravailResource(PostesDeTravailApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les postes de travail",
    description = """
    La page demandee, triee par libelle. Le filtre sur la nature est facultatif ; absent, il ne filtre rien.
    """
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, triee par libelle.")
  Page<RestPosteDeTravail> list(
    @RequestParam(required = false) String nature,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<PosteDeTravail> resultat = applicationService.list(nature(nature), new Pageable(page, size));

    return Page.<RestPosteDeTravail>builder()
      .content(resultat.content().stream().map(RestPosteDeTravail::from).toList())
      .currentPage(resultat.currentPage())
      .pageSize(resultat.pageSize())
      .totalElementsCount(resultat.totalElementsCount());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Declarer un poste de travail")
  @ApiResponse(responseCode = "201", description = "Le poste est declare.")
  @ApiResponse(responseCode = "409", description = "Un autre poste porte deja ce libelle.")
  RestPosteDeTravail create(@RequestBody @Valid RestCreationPosteDeTravail request) {
    return RestPosteDeTravail.from(applicationService.create(request.toDomain()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consulter un poste de travail")
  @ApiResponse(responseCode = "404", description = "Poste de travail introuvable.")
  RestPosteDeTravail get(@PathVariable UUID id) {
    return RestPosteDeTravail.from(applicationService.get(new PosteDeTravailId(id)));
  }

  @PutMapping("/{id}")
  @Operation(
    summary = "Reviser un poste de travail",
    description = """
    Le poste renomme s'affiche renomme partout : rien n'en est copie sur les operateurs, qui n'en retiennent que
    l'identifiant.
    """
  )
  @ApiResponse(responseCode = "404", description = "Poste de travail introuvable.")
  @ApiResponse(responseCode = "409", description = "Un autre poste porte deja ce libelle.")
  RestPosteDeTravail update(@PathVariable UUID id, @RequestBody @Valid RestModificationPosteDeTravail request) {
    return RestPosteDeTravail.from(applicationService.update(request.toDomain(new PosteDeTravailId(id))));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Supprimer un poste de travail",
    description = """
    Refuse tant qu'un operateur y est habilite : le supprimer le laisserait pointer sur du vide. Le retirer de leurs
    habilitations d'abord.
    """
  )
  @ApiResponse(responseCode = "204", description = "Le poste est supprime.")
  @ApiResponse(responseCode = "404", description = "Poste de travail introuvable.")
  @ApiResponse(responseCode = "409", description = "Des operateurs sont encore habilites sur ce poste.")
  void delete(@PathVariable UUID id) {
    applicationService.delete(new PosteDeTravailId(id));
  }

  private static Optional<NatureDeTravail> nature(String nature) {
    return Optional.ofNullable(nature).map(NatureDeTravail::new);
  }
}

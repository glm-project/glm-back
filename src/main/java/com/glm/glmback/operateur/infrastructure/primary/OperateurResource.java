package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.application.OperateursApplicationService;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.PosteHabilitableId;
import com.glm.glmback.operateur.domain.ProfilDOperateur;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
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
@RequestMapping("/api/operateurs")
@Tag(
  name = "Operateurs",
  description = """
  Le referentiel des personnes qui pointent, et des postes sur lesquels elles sont habilitees.

  Les metiers d'un operateur ne se saisissent pas : ils se deduisent des natures de ses postes. Un operateur habilite
  sur le poste de soudure et sur un tour ressort soudeur et tourneur.

  En entree, l'operateur porte des identifiants de postes ; en sortie, il porte les postes resolus et ses natures, pour
  qu'un ecran se serve d'un seul appel.
  """
)
class OperateurResource {

  private final OperateursApplicationService applicationService;

  OperateurResource(OperateursApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les operateurs",
    description = """
    La page demandee, triee par nom puis prenom. Le filtre sur le poste repond a la question du pupitre : qui est
    habilite sur ce poste ? Absent, il ne filtre rien.
    """
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, triee par nom puis prenom.")
  RestPage<RestOperateur> list(
    @RequestParam(required = false) UUID poste,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<ProfilDOperateur> resultat = applicationService.list(poste(poste), new Pageable(page, size));

    return RestPage.from(resultat, RestOperateur::from);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Declarer un operateur")
  @ApiResponse(responseCode = "201", description = "L'operateur est declare.")
  @ApiResponse(responseCode = "404", description = "Un des postes references est introuvable.")
  @ApiResponse(responseCode = "409", description = "Cette identite ou ce matricule appartient deja a un autre operateur.")
  RestOperateur create(@RequestBody @Valid RestCreationOperateur request) {
    return RestOperateur.from(applicationService.create(request.toDomain()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consulter un operateur")
  @ApiResponse(responseCode = "404", description = "Operateur introuvable.")
  RestOperateur get(@PathVariable UUID id) {
    return RestOperateur.from(applicationService.get(new OperateurId(id)));
  }

  @PutMapping("/{id}")
  @Operation(
    summary = "Reviser un operateur",
    description = "La liste de postes fournie remplace la precedente ; un matricule laisse vide est retire."
  )
  @ApiResponse(responseCode = "404", description = "Operateur ou poste reference introuvable.")
  @ApiResponse(responseCode = "409", description = "Cette identite ou ce matricule appartient deja a un autre operateur.")
  RestOperateur update(@PathVariable UUID id, @RequestBody @Valid RestModificationOperateur request) {
    return RestOperateur.from(applicationService.update(request.toDomain(new OperateurId(id))));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Supprimer un operateur")
  @ApiResponse(responseCode = "204", description = "L'operateur est supprime.")
  @ApiResponse(responseCode = "404", description = "Operateur introuvable.")
  @ApiResponse(responseCode = "409", description = "Du temps est pointe au nom de cet operateur.")
  void delete(@PathVariable UUID id) {
    applicationService.delete(new OperateurId(id));
  }

  private static Optional<PosteHabilitableId> poste(UUID poste) {
    return Optional.ofNullable(poste).map(PosteHabilitableId::new);
  }
}

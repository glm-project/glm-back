package com.glm.glmback.naturedetravail.infrastructure.primary;

import com.glm.glmback.naturedetravail.application.NaturesDeTravailApplicationService;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/natures-de-travail")
@Tag(
  name = "Natures de travail",
  description = """
  Les metiers exerces dans l'atelier : soudage, tournage, fraisage, dessin. L'entreprise les declare une fois, et les
  postes de travail les choisissent dans cette liste.

  Deux natures ne peuvent pas porter le meme libelle a la casse, aux accents ou aux espaces pres : « Soudage » et
  « soudâge » sont la meme.

  Renommer une nature ne modifie qu'elle : son nouveau libelle s'affiche partout, rapports passes compris.

  Le gestionnaire declare et renomme les natures ; l'operateur (role USER) les consulte.
  """
)
class NatureDeTravailResource {

  private final NaturesDeTravailApplicationService applicationService;

  NatureDeTravailResource(NaturesDeTravailApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les natures de travail",
    description = """
    La page demandee, dans l'ordre alphabetique sans egard aux accents ni a la casse. Chaque nature dit si elle sert
    deja : l'ecran ne propose alors pas de la supprimer.
    """
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, par ordre alphabetique.")
  RestPage<RestNatureDeTravail> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return RestPage.from(applicationService.list(new Pageable(page, size)), RestNatureDeTravail::from);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Declarer une nature de travail", description = "La nature est declaree sans poste ni pointage.")
  @ApiResponse(responseCode = "201", description = "La nature est declaree.")
  @ApiResponse(responseCode = "409", description = "Une nature porte deja ce libelle, a la casse ou aux accents pres.")
  RestNatureDeTravail declare(@RequestBody @Valid RestLibelleDeNature request) {
    return RestNatureDeTravail.declaree(applicationService.declare(request.toDomain()));
  }

  @PutMapping("/{id}")
  @Operation(
    summary = "Renommer une nature de travail",
    description = """
    Seule la nature change : postes et pointages n'en retiennent que l'identifiant, et le nouveau libelle s'affiche
    partout, rapports passes compris. Changer la casse ou les accents du libelle est permis.
    """
  )
  @ApiResponse(responseCode = "200", description = "La nature est renommee.")
  @ApiResponse(responseCode = "404", description = "Nature de travail introuvable.")
  @ApiResponse(responseCode = "409", description = "Une autre nature porte deja ce libelle, a la casse ou aux accents pres.")
  RestNatureDeTravail renomme(@PathVariable UUID id, @RequestBody @Valid RestLibelleDeNature request) {
    return RestNatureDeTravail.from(applicationService.renomme(new NatureDeTravailId(id), request.toDomain()));
  }
}

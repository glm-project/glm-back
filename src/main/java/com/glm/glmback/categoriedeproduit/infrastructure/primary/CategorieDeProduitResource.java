package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import com.glm.glmback.categoriedeproduit.application.CategoriesDeProduitApplicationService;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping("/api/categories-de-produit")
@Tag(
  name = "Categories de produit",
  description = """
  Les familles dans lesquelles l'entreprise range ce qu'elle fabrique : des moules et des OF chez le client de
  reference, autre chose ailleurs. Une entreprise neuve n'en a aucune.

  Le code d'une categorie est a la fois son libelle et le prefixe du nom des produits qui s'y creent
  (MOULE-2026-000001). Il ne se renomme donc jamais.

  Une categorie se supprime tant qu'aucun produit n'y est range.

  Le gestionnaire declare, reordonne et supprime les categories ; l'operateur (role USER) les consulte.
  """
)
class CategorieDeProduitResource {

  private final CategoriesDeProduitApplicationService applicationService;

  CategorieDeProduitResource(CategoriesDeProduitApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les categories de produit",
    description = """
    La page demandee, dans l'ordre d'affichage choisi par l'entreprise. Chaque categorie dit si des produits y sont
    ranges : l'ecran ne propose alors pas de la supprimer.
    """
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, dans l'ordre d'affichage.")
  RestPage<RestCategorieDeProduit> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return RestPage.from(applicationService.list(new Pageable(page, size)), RestCategorieDeProduit::from);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Declarer une categorie de produit", description = "La categorie se range en dernier, sans produit.")
  @ApiResponse(responseCode = "201", description = "La categorie est declaree.")
  @ApiResponse(responseCode = "409", description = "Une categorie porte deja ce code.")
  RestCategorieDeProduit create(@RequestBody @Valid RestCreationCategorieDeProduit request) {
    return RestCategorieDeProduit.declaree(applicationService.create(request.toDomain()));
  }

  @PutMapping("/ordre")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Reordonner les categories de produit",
    description = """
    L'ordre est donne en entier : chaque categorie de l'entreprise, une fois et une seule, de la premiere a la
    derniere. C'est celui des boutons de creation, des filtres et des zones du pupitre.
    """
  )
  @ApiResponse(responseCode = "204", description = "L'ordre est enregistre.")
  @ApiResponse(
    responseCode = "409",
    description = "L'ordre omet une categorie, en cite une deux fois ou en cite une inconnue — par exemple declaree ou supprimee entre-temps."
  )
  void reordonne(@RequestBody @Valid RestOrdreDesCategories request) {
    applicationService.reordonne(request.toDomain());
  }

  @DeleteMapping("/{code}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Supprimer une categorie de produit",
    description = """
    Refuse tant qu'un produit y est range : son nom porte le code de la categorie.
    """
  )
  @ApiResponse(responseCode = "204", description = "La categorie est supprimee.")
  @ApiResponse(responseCode = "404", description = "Categorie introuvable.")
  @ApiResponse(responseCode = "409", description = "Des produits sont ranges dans cette categorie.")
  void delete(@PathVariable @Pattern(regexp = "^[A-Z]{1,10}$") String code) {
    applicationService.delete(new CodeDeCategorie(code));
  }
}

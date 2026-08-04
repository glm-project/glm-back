package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.application.ElementDeFabricationApplicationService;
import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Nom;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabrication;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.elementdefabrication.domain.Produit;
import com.glm.glmback.elementdefabrication.domain.ProduitId;
import com.glm.glmback.elementdefabrication.domain.Titre;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.validation.Valid;
import java.time.Instant;
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
@RequestMapping("/api/elements-de-fabrication")
class ElementDeFabricationResource {

  private final ElementDeFabricationApplicationService applicationService;

  ElementDeFabricationResource(ElementDeFabricationApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @PostMapping("/ordres-de-fabrication")
  @ResponseStatus(HttpStatus.CREATED)
  ElementDeFabricationReponse creerOrdreDeFabrication(@RequestBody @Valid CreationElementDeFabricationRequest request) {
    return versReponse(
      applicationService.creerOrdreDeFabrication(nomDe(request.nom()), new Titre(request.titre()), new Description(request.description()))
    );
  }

  @PostMapping("/produits")
  @ResponseStatus(HttpStatus.CREATED)
  ElementDeFabricationReponse creerProduit(@RequestBody @Valid CreationElementDeFabricationRequest request) {
    return versReponse(
      applicationService.creerProduit(nomDe(request.nom()), new Titre(request.titre()), new Description(request.description()))
    );
  }

  @GetMapping("/ordres-de-fabrication/{id}")
  ElementDeFabricationReponse obtenirOrdreDeFabrication(@PathVariable UUID id) {
    return versReponse(applicationService.obtenir(new OrdreDeFabricationId(id)));
  }

  @GetMapping("/produits/{id}")
  ElementDeFabricationReponse obtenirProduit(@PathVariable UUID id) {
    return versReponse(applicationService.obtenir(new ProduitId(id)));
  }

  @PutMapping("/ordres-de-fabrication/{id}")
  ElementDeFabricationReponse modifierOrdreDeFabrication(
    @PathVariable UUID id,
    @RequestBody @Valid ModificationElementDeFabricationRequest request
  ) {
    return versReponse(
      applicationService.modifier(new OrdreDeFabricationId(id), new Titre(request.titre()), new Description(request.description()))
    );
  }

  @PutMapping("/produits/{id}")
  ElementDeFabricationReponse modifierProduit(@PathVariable UUID id, @RequestBody @Valid ModificationElementDeFabricationRequest request) {
    return versReponse(applicationService.modifier(new ProduitId(id), new Titre(request.titre()), new Description(request.description())));
  }

  @DeleteMapping("/ordres-de-fabrication/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void supprimerOrdreDeFabrication(@PathVariable UUID id) {
    applicationService.supprimer(new OrdreDeFabricationId(id));
  }

  @DeleteMapping("/produits/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void supprimerProduit(@PathVariable UUID id) {
    applicationService.supprimer(new ProduitId(id));
  }

  @GetMapping
  Page<ElementDeFabricationReponse> lister(
    @RequestParam Instant debut,
    @RequestParam Instant fin,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<ElementDeFabrication> resultat = applicationService.lister(new Periode(debut, fin), new Pageable(page, size));

    return Page.<ElementDeFabricationReponse>builder()
      .content(resultat.content().stream().map(this::versReponse).toList())
      .currentPage(resultat.currentPage())
      .pageSize(resultat.pageSize())
      .totalElementsCount(resultat.totalElementsCount());
  }

  private Optional<Nom> nomDe(String nom) {
    return Optional.ofNullable(nom).map(Nom::new);
  }

  private ElementDeFabricationReponse versReponse(ElementDeFabrication element) {
    return switch (element) {
      case OrdreDeFabrication ordre -> new ElementDeFabricationReponse(
        "ORDRE_DE_FABRICATION",
        ordre.id().uuid(),
        ordre.nom().value(),
        ordre.titre().value(),
        ordre.description().value(),
        ordre.dateDeCreation(),
        ordre.dateDeModification()
      );
      case Produit produit -> new ElementDeFabricationReponse(
        "PRODUIT",
        produit.id().uuid(),
        produit.nom().value(),
        produit.titre().value(),
        produit.description().value(),
        produit.dateDeCreation(),
        produit.dateDeModification()
      );
    };
  }
}

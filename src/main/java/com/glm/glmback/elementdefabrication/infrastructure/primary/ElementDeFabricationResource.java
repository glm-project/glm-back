package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.application.ElementDeFabricationApplicationService;
import com.glm.glmback.elementdefabrication.domain.Categorie;
import com.glm.glmback.elementdefabrication.domain.CycleDeVie;
import com.glm.glmback.elementdefabrication.domain.CycleDemande;
import com.glm.glmback.elementdefabrication.domain.ElementDansSonCycle;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.FiltreDesElements;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.elementdefabrication.domain.Recherche;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/elements-de-fabrication")
class ElementDeFabricationResource {

  private final ElementDeFabricationApplicationService applicationService;

  ElementDeFabricationResource(ElementDeFabricationApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  RestElementDeFabrication create(@RequestBody @Valid RestCreationElementDeFabrication request) {
    return RestElementDeFabrication.from(applicationService.create(request.toDomain()));
  }

  @GetMapping("/{id}")
  RestElementDeFabrication get(@PathVariable UUID id) {
    return RestElementDeFabrication.from(applicationService.get(new ElementDeFabricationId(id)));
  }

  @PutMapping("/{id}")
  RestElementDeFabrication update(@PathVariable UUID id, @RequestBody @Valid RestModificationElementDeFabrication request) {
    return RestElementDeFabrication.from(applicationService.update(request.toDomain(new ElementDeFabricationId(id))));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id) {
    applicationService.delete(new ElementDeFabricationId(id));
  }

  @GetMapping
  RestPage<RestElementDeFabrication> list(
    @Parameter(description = "Debut de la periode de creation, a donner avec fin : une seule borne ne filtre rien.") @RequestParam Optional<
      Instant
    > debut,
    @Parameter(description = "Fin de la periode de creation, a donner avec debut.") @RequestParam Optional<Instant> fin,
    @Parameter(
      description = "Ne lire que les elements de ce cycle, tries par creation, mise a l'atelier ou cloture decroissante."
    ) @RequestParam Optional<CycleDeVie> cycle,
    @Parameter(
      description = "Debut de la periode de cloture, a donner avec clotureFin, et avec cycle=CLOTURE seulement (sinon 400)."
    ) @RequestParam Optional<Instant> clotureDebut,
    @Parameter(
      description = "Fin de la periode de cloture, a donner avec clotureDebut, et avec cycle=CLOTURE seulement (sinon 400)."
    ) @RequestParam Optional<Instant> clotureFin,
    @Parameter(description = "Ne lire que les elements de cette categorie, par son code.") @RequestParam Optional<
      @Pattern(regexp = "^[A-Z]{1,10}$") String
    > categorie,
    @Parameter(
      description = "Texte cherche dans le nom, la reference et la description, sans tenir compte de la casse. Vide, il ne filtre rien."
    ) @RequestParam Optional<String> recherche,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    FiltreDesElements filtre = new FiltreDesElements(periode(debut, fin), categorie.map(Categorie::new), Recherche.de(recherche));
    Optional<CycleDemande> demande = CycleDemande.de(cycle, periode(clotureDebut, clotureFin));
    Page<ElementDansSonCycle> resultat = applicationService.list(filtre, demande, new Pageable(page, size));

    return RestPage.from(resultat, RestElementDeFabrication::from);
  }

  @GetMapping("/compteurs")
  @Operation(
    summary = "Compter les elements de chaque cycle",
    description = "Pour les badges des onglets : memes filtres que la liste, la periode de cloture ne comptant que les clotures."
  )
  RestCompteursDesCycles compte(
    @Parameter(description = "Debut de la periode de creation, a donner avec fin.") @RequestParam Optional<Instant> debut,
    @Parameter(description = "Fin de la periode de creation, a donner avec debut.") @RequestParam Optional<Instant> fin,
    @Parameter(description = "Ne compter que les elements de cette categorie, par son code.") @RequestParam Optional<
      @Pattern(regexp = "^[A-Z]{1,10}$") String
    > categorie,
    @Parameter(description = "Texte cherche dans le nom, la reference et la description.") @RequestParam Optional<String> recherche,
    @Parameter(description = "Debut de la periode de cloture, pour le seul compteur des clotures.") @RequestParam Optional<
      Instant
    > clotureDebut,
    @Parameter(description = "Fin de la periode de cloture, pour le seul compteur des clotures.") @RequestParam Optional<Instant> clotureFin
  ) {
    FiltreDesElements filtre = new FiltreDesElements(periode(debut, fin), categorie.map(Categorie::new), Recherche.de(recherche));

    return RestCompteursDesCycles.from(applicationService.compte(filtre, periode(clotureDebut, clotureFin)));
  }

  /**
   * Comme la liste de l'atelier, une periode a une seule borne ne filtre rien.
   */
  private static Optional<Periode> periode(Optional<Instant> debut, Optional<Instant> fin) {
    return debut.flatMap(depuis -> fin.map(jusqua -> new Periode(depuis, jusqua)));
  }
}

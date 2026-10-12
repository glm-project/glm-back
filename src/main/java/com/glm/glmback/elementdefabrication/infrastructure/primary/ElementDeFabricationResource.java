package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.application.ElementDeFabricationApplicationService;
import com.glm.glmback.elementdefabrication.domain.CycleDeVie;
import com.glm.glmback.elementdefabrication.domain.ElementDansSonCycle;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Parameter;
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
    @RequestParam Instant debut,
    @RequestParam Instant fin,
    @Parameter(
      description = "Ne lire que les elements de ce cycle, tries par creation, mise a l'atelier ou cloture decroissante."
    ) @RequestParam Optional<CycleDeVie> cycle,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<ElementDansSonCycle> resultat = applicationService.list(Optional.of(new Periode(debut, fin)), cycle, new Pageable(page, size));

    return RestPage.from(resultat, RestElementDeFabrication::from);
  }
}

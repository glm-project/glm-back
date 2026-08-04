package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.application.ElementDeFabricationApplicationService;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.validation.Valid;
import java.time.Instant;
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

  @GetMapping("/{type}/{id}")
  RestElementDeFabrication get(@PathVariable RestTypeElementDeFabrication type, @PathVariable UUID id) {
    return RestElementDeFabrication.from(applicationService.get(type.toDomain(id)));
  }

  @PutMapping("/{type}/{id}")
  RestElementDeFabrication update(
    @PathVariable RestTypeElementDeFabrication type,
    @PathVariable UUID id,
    @RequestBody @Valid RestModificationElementDeFabrication request
  ) {
    return RestElementDeFabrication.from(applicationService.update(request.toDomain(type.toDomain(id))));
  }

  @DeleteMapping("/{type}/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable RestTypeElementDeFabrication type, @PathVariable UUID id) {
    applicationService.delete(type.toDomain(id));
  }

  @GetMapping
  Page<RestElementDeFabrication> list(
    @RequestParam Instant debut,
    @RequestParam Instant fin,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<ElementDeFabrication> resultat = applicationService.list(new Periode(debut, fin), new Pageable(page, size));

    return Page.<RestElementDeFabrication>builder()
      .content(resultat.content().stream().map(RestElementDeFabrication::from).toList())
      .currentPage(resultat.currentPage())
      .pageSize(resultat.pageSize())
      .totalElementsCount(resultat.totalElementsCount());
  }
}

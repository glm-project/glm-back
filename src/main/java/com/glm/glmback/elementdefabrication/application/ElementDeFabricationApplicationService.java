package com.glm.glmback.elementdefabrication.application;

import com.glm.glmback.elementdefabrication.domain.CategoriesDeclarees;
import com.glm.glmback.elementdefabrication.domain.CompteurDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.CycleDeVie;
import com.glm.glmback.elementdefabrication.domain.ElementDansSonCycle;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToCreate;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToUpdate;
import com.glm.glmback.elementdefabrication.domain.ElementsDansLeurCycle;
import com.glm.glmback.elementdefabrication.domain.ElementsDansLeurCycleCriteria;
import com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationService;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElementDeFabricationApplicationService {

  private final ElementsDeFabricationService elementsDeFabrication;
  private final ElementsDansLeurCycle elementsDansLeurCycle;
  private final Clock clock;

  public ElementDeFabricationApplicationService(
    ElementDeFabricationRepository repository,
    CompteurDElementsDeFabrication compteur,
    CategoriesDeclarees categories,
    ElementsDansLeurCycle elementsDansLeurCycle,
    Clock clock
  ) {
    this.elementsDansLeurCycle = elementsDansLeurCycle;
    this.clock = clock;
    this.elementsDeFabrication = ElementsDeFabricationService.builder()
      .repository(repository)
      .compteur(compteur)
      .categories(categories)
      .clock(clock);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ElementDeFabrication create(ElementDeFabricationToCreate toCreate) {
    return elementsDeFabrication.create(toCreate);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public ElementDeFabrication get(ElementDeFabricationId id) {
    return elementsDeFabrication.get(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<ElementDansSonCycle> list(Optional<Periode> creation, Optional<CycleDeVie> cycle, Pageable pageable) {
    return elementsDansLeurCycle.list(new ElementsDansLeurCycleCriteria(creation, cycle, clock.now()), pageable);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ElementDeFabrication update(ElementDeFabricationToUpdate toUpdate) {
    return elementsDeFabrication.update(toUpdate);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public void delete(ElementDeFabricationId id) {
    elementsDeFabrication.delete(id);
  }
}

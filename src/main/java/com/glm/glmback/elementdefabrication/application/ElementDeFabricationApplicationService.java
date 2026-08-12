package com.glm.glmback.elementdefabrication.application;

import com.glm.glmback.elementdefabrication.domain.CompteurDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToCreate;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToUpdate;
import com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationService;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.elementdefabrication.domain.PrefixesDElementsDeFabrication;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElementDeFabricationApplicationService {

  private final ElementsDeFabricationService elementsDeFabrication;

  public ElementDeFabricationApplicationService(
    ElementDeFabricationRepository repository,
    CompteurDElementsDeFabrication compteur,
    PrefixesDElementsDeFabrication prefixes,
    Clock clock
  ) {
    this.elementsDeFabrication = ElementsDeFabricationService.builder()
      .repository(repository)
      .compteur(compteur)
      .prefixes(prefixes)
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
  public Page<ElementDeFabrication> list(Periode periode, Pageable pageable) {
    return elementsDeFabrication.list(periode, pageable);
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

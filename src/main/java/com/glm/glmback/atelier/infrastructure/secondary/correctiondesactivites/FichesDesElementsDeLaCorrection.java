package com.glm.glmback.atelier.infrastructure.secondary.correctiondesactivites;

import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.correctiondesactivites.FicheDElement;
import com.glm.glmback.atelier.domain.correctiondesactivites.FichesDElement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les fiches des elements, lues dans la table du referentiel sans importer son code, en une seule requete.
 */
@Repository
class FichesDesElementsDeLaCorrection implements FichesDElement {

  private final SpringDataFichesDElementDeLaCorrectionRepository elements;

  FichesDesElementsDeLaCorrection(SpringDataFichesDElementDeLaCorrectionRepository elements) {
    this.elements = elements;
  }

  @Override
  public List<FicheDElement> parIds(Set<ElementEngageId> ids) {
    return elements
      .findByIdIn(ids.stream().map(ElementEngageId::uuid).collect(Collectors.toSet()))
      .stream()
      .map(FicheDElementDeLaCorrectionEntity::toDomain)
      .toList();
  }
}

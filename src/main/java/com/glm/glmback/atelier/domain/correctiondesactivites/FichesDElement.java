package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ElementEngageId;
import java.util.List;
import java.util.Set;

/**
 * Le referentiel des elements de fabrication, lu sans importer le contexte qui le possede. Un element supprime n'y figure
 * plus : sa reference et sa description sont alors absentes.
 */
@FunctionalInterface
public interface FichesDElement {
  List<FicheDElement> parIds(Set<ElementEngageId> ids);
}

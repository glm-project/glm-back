package com.glm.glmback.syntheseheures.domain;

import java.util.List;
import java.util.Set;

/**
 * Le referentiel des elements de fabrication, lu sans importer le contexte qui le possede. Un element supprime n'y
 * figure plus : sa reference et sa description sont alors absentes du releve.
 */
@FunctionalInterface
public interface ElementsDeFabrication {
  List<FicheDElement> parIds(Set<ElementId> ids);
}

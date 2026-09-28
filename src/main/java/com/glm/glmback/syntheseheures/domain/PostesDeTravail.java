package com.glm.glmback.syntheseheures.domain;

import java.util.List;
import java.util.Set;

/**
 * Le referentiel des postes de travail, lu sans importer le contexte qui le possede. Un poste qui a servi a pointer ne
 * se supprime jamais.
 */
@FunctionalInterface
public interface PostesDeTravail {
  List<PosteConnu> parIds(Set<PosteDeTravailId> ids);
}

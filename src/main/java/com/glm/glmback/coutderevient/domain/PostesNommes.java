package com.glm.glmback.coutderevient.domain;

import java.util.List;
import java.util.Set;

/** Le referentiel des postes de travail, atteint par lot d'identifiants. */
@FunctionalInterface
public interface PostesNommes {
  List<PosteNomme> postes(Set<PosteDeTravailId> postes);
}

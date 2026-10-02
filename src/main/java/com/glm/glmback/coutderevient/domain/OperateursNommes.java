package com.glm.glmback.coutderevient.domain;

import java.util.List;
import java.util.Set;

/** Le referentiel des operateurs, atteint par lot d'identifiants. */
@FunctionalInterface
public interface OperateursNommes {
  List<OperateurNomme> operateurs(Set<OperateurId> operateurs);
}

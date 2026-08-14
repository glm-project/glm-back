package com.glm.glmback.feuilledetemps.domain;

import java.util.Optional;

/**
 * Le referentiel des operateurs, lu sans jamais importer le contexte qui le possede.
 */
public interface OperateursConnus {
  Optional<OperateurConnu> get(OperateurId id);
}

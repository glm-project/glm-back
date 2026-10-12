package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;

/**
 * Lit les elements avec leur passage en atelier, sur les tables de l'atelier, sans importer son code.
 */
public interface ElementsDansLeurCycle {
  Page<ElementDansSonCycle> list(ElementsDansLeurCycleCriteria criteria, Pageable pageable);
}

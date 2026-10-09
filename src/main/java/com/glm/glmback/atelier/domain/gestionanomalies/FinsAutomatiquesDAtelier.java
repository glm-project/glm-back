package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;

/**
 * Lecture paginee des activites projetees que rien n'a terminees et dont l'echeance est atteinte a l'instant
 * d'evaluation, borne comprise.
 */
public interface FinsAutomatiquesDAtelier {
  Page<FinAutomatiqueEnListe> list(AnomaliesDAtelierCriteria criteria, Instant evaluation, Pageable pageable);
}

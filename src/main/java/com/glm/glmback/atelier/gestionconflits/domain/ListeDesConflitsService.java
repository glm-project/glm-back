package com.glm.glmback.atelier.gestionconflits.domain;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.stream.Collectors;

/** Une acquisition des projections et une resolution en lot de chacune des deux ressources. */
public final class ListeDesConflitsService {

  private final ConflitsDAtelier conflits;
  private final OperateursConnus operateurs;
  private final PostesConnus postes;

  public ListeDesConflitsService(ConflitsDAtelier conflits, OperateursConnus operateurs, PostesConnus postes) {
    this.conflits = conflits;
    this.operateurs = operateurs;
    this.postes = postes;
  }

  public LectureDesConflits list(ConflitsDAtelierCriteria criteria, Pageable pageable) {
    var page = conflits.list(criteria, pageable);
    var operateursAResoudre = page
      .content()
      .stream()
      .map(ligne -> ligne.cle().operateur())
      .collect(Collectors.toSet());
    var postesAResoudre = page
      .content()
      .stream()
      .flatMap(ligne -> ligne.cle().poste().stream())
      .collect(Collectors.toSet());
    return new LectureDesConflits(page, AnnuaireDAtelier.de(operateurs.parIds(operateursAResoudre), postes.parIds(postesAResoudre)));
  }
}

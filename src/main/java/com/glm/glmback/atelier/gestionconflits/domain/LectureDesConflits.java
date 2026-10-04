package com.glm.glmback.atelier.gestionconflits.domain;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.shared.error.domain.Assert;
import com.glm.glmback.shared.pagination.domain.Page;

public record LectureDesConflits(Page<ConflitEnListe> page, AnnuaireDAtelier annuaire) {
  public LectureDesConflits {
    Assert.notNull("page", page);
    Assert.notNull("annuaire", annuaire);
  }
}

package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.shared.error.domain.Assert;
import com.glm.glmback.shared.pagination.domain.Page;

public record LectureDesFinsAutomatiques(Page<FinAutomatiqueEnListe> page, AnnuaireDAtelier annuaire) {
  public LectureDesFinsAutomatiques {
    Assert.notNull("page", page);
    Assert.notNull("annuaire", annuaire);
  }
}

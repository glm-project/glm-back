package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;

public record LectureDeSupervision(Instant evaluation, List<OperateurDeSupervision> operateurs, List<ActiviteDeSupervision> activites) {
  public LectureDeSupervision {
    Assert.notNull("evaluation", evaluation);
    Assert.field("operateurs", operateurs).notNull().noNullElement();
    operateurs = List.copyOf(operateurs);
    Assert.field("activites", activites).notNull().noNullElement();
    activites = List.copyOf(activites);
  }
}

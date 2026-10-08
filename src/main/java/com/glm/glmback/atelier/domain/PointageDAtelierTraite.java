package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le suivi apres un pointage, et ce qu'est devenu ce pointage.
 */
public record PointageDAtelierTraite(SuiviDAtelier suivi, IssueDePointage issue) {
  public PointageDAtelierTraite {
    Assert.notNull("suivi", suivi);
    Assert.notNull("issue", issue);
  }
}

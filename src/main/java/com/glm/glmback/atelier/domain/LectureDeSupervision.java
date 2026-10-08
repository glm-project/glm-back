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

  private LectureDeSupervision(Builder b) {
    this(b.evaluation, b.operateurs, b.activites);
  }

  public static EvaluationStep builder() {
    return new Builder();
  }

  private static final class Builder implements EvaluationStep, OperateursStep, ActivitesStep {

    private Instant evaluation;
    private List<OperateurDeSupervision> operateurs;
    private List<ActiviteDeSupervision> activites;

    public OperateursStep evaluation(Instant evaluation) {
      this.evaluation = evaluation;
      return this;
    }

    public ActivitesStep operateurs(List<OperateurDeSupervision> operateurs) {
      this.operateurs = operateurs;
      return this;
    }

    public LectureDeSupervision activites(List<ActiviteDeSupervision> activites) {
      this.activites = activites;
      return new LectureDeSupervision(this);
    }
  }

  public interface EvaluationStep {
    OperateursStep evaluation(Instant evaluation);
  }

  public interface OperateursStep {
    ActivitesStep operateurs(List<OperateurDeSupervision> operateurs);
  }

  public interface ActivitesStep {
    LectureDeSupervision activites(List<ActiviteDeSupervision> activites);
  }
}

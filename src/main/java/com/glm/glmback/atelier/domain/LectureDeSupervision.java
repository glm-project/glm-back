package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;

public record LectureDeSupervision(
  Instant evaluation,
  List<OperateurDeSupervision> operateurs,
  List<ActiviteDeSupervision> activites,
  List<SequenceEnConflitDeSupervision> sequencesEnConflit
) {
  public LectureDeSupervision {
    Assert.notNull("evaluation", evaluation);
    Assert.field("operateurs", operateurs).notNull().noNullElement();
    operateurs = List.copyOf(operateurs);
    Assert.field("activites", activites).notNull().noNullElement();
    activites = List.copyOf(activites);
    Assert.field("sequences en conflit", sequencesEnConflit).notNull().noNullElement();
    sequencesEnConflit = List.copyOf(sequencesEnConflit);
  }

  private LectureDeSupervision(Builder b) {
    this(b.evaluation, b.operateurs, b.activites, b.sequencesEnConflit);
  }

  public static EvaluationStep builder() {
    return new Builder();
  }

  private static final class Builder implements EvaluationStep, OperateursStep, ActivitesStep, SequencesStep {

    private Instant evaluation;
    private List<OperateurDeSupervision> operateurs;
    private List<ActiviteDeSupervision> activites;
    private List<SequenceEnConflitDeSupervision> sequencesEnConflit;

    public OperateursStep evaluation(Instant evaluation) {
      this.evaluation = evaluation;
      return this;
    }

    public ActivitesStep operateurs(List<OperateurDeSupervision> operateurs) {
      this.operateurs = operateurs;
      return this;
    }

    public SequencesStep activites(List<ActiviteDeSupervision> activites) {
      this.activites = activites;
      return this;
    }

    public LectureDeSupervision sequencesEnConflit(List<SequenceEnConflitDeSupervision> sequencesEnConflit) {
      this.sequencesEnConflit = sequencesEnConflit;
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
    SequencesStep activites(List<ActiviteDeSupervision> activites);
  }

  public interface SequencesStep {
    LectureDeSupervision sequencesEnConflit(List<SequenceEnConflitDeSupervision> sequencesEnConflit);
  }
}

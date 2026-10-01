package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

public record SequenceEnConflitDeSupervision(
  EvenementDAtelierId id,
  OperateurId operateur,
  Optional<PosteDeSupervision> poste,
  List<DescriptionDActiviteDeSupervision> activites
) {
  public SequenceEnConflitDeSupervision {
    Assert.notNull("sequence", id);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste", poste);
    Assert.field("activites", activites).notNull().noNullElement();
    activites = List.copyOf(activites);
  }

  public static IdStep builder() {
    return id -> operateur -> poste -> activites -> new SequenceEnConflitDeSupervision(id, operateur, poste, activites);
  }

  public interface IdStep {
    OperateurStep id(EvenementDAtelierId id);
  }

  public interface OperateurStep {
    PosteStep operateur(OperateurId operateur);
  }

  public interface PosteStep {
    ActivitesStep poste(Optional<PosteDeSupervision> poste);
  }

  public interface ActivitesStep {
    SequenceEnConflitDeSupervision activites(List<DescriptionDActiviteDeSupervision> activites);
  }
}

package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Les faits contradictoires projetes par atelier, sans interpretation du lecteur. */
public record SequenceEnConflit(
  ElementId element,
  OperateurId operateur,
  Optional<PosteDeTravailId> poste,
  List<ActiviteId> activites,
  List<UUID> pointages
) {
  public SequenceEnConflit {
    Assert.notNull("element", element);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste", poste);
    Assert.field("activites", activites).notNull().noNullElement();
    Assert.field("pointages", pointages).notNull().noNullElement();
    activites = List.copyOf(activites);
    pointages = List.copyOf(pointages);
  }

  public static ElementBuilder builder() {
    return element ->
      operateur -> poste -> activites -> pointages -> new SequenceEnConflit(element, operateur, poste, activites, pointages);
  }

  public boolean concerne(Set<ActiviteId> responsables) {
    return activites.stream().anyMatch(responsables::contains);
  }

  public interface ElementBuilder {
    OperateurBuilder element(ElementId element);
  }

  public interface OperateurBuilder {
    PosteBuilder operateur(OperateurId operateur);
  }

  public interface PosteBuilder {
    ActivitesBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface ActivitesBuilder {
    PointagesBuilder activites(List<ActiviteId> activites);
  }

  public interface PointagesBuilder {
    SequenceEnConflit pointages(List<UUID> pointages);
  }
}

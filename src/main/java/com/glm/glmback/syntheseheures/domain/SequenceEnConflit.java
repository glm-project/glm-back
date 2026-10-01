package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Les identites de la sequence irresolue, y compris sans activite interpretee ni poste. */
public record SequenceEnConflit(
  ElementId element,
  Optional<PosteDeTravailId> poste,
  List<ActiviteId> activites,
  List<PointageId> pointages
) {
  public SequenceEnConflit {
    Assert.notNull("element", element);
    Assert.notNull("poste", poste);
    Assert.field("activites", activites).notNull().noNullElement();
    Assert.field("pointages", pointages).notNull().noNullElement();
  }

  public static ElementBuilder builder() {
    return element -> poste -> activites -> pointages -> new SequenceEnConflit(element, poste, activites, pointages);
  }

  boolean concerne(Set<ActiviteId> activitesRendues, Set<PointageId> pointagesRendus) {
    return activites.stream().anyMatch(activitesRendues::contains) || pointages.stream().anyMatch(pointagesRendus::contains);
  }

  public interface ElementBuilder {
    PosteBuilder element(ElementId element);
  }

  public interface PosteBuilder {
    ActivitesBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface ActivitesBuilder {
    PointagesBuilder activites(List<ActiviteId> activites);
  }

  public interface PointagesBuilder {
    SequenceEnConflit pointages(List<PointageId> pointages);
  }
}

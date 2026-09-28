package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un pointage de l'operateur sur un element, tel qu'il figure dans le journal brut du jour : le geste, l'element, le
 * poste s'il a ete pointe, la nature figee a la saisie, et l'heure.
 */
public record PointageDElement(
  TypeDEvenementDAtelier type,
  ElementId element,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  Instant dateDeSurvenue
) implements PointageDuJour {
  public PointageDElement {
    Assert.notNull("type", type);
    Assert.notNull("element", element);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("date de survenue", dateDeSurvenue);
  }

  static PointageDElementTypeBuilder builder() {
    return type -> element -> poste -> nature -> dateDeSurvenue -> new PointageDElement(type, element, poste, nature, dateDeSurvenue);
  }

  interface PointageDElementTypeBuilder {
    PointageDElementElementBuilder type(TypeDEvenementDAtelier type);
  }

  interface PointageDElementElementBuilder {
    PointageDElementPosteBuilder element(ElementId element);
  }

  interface PointageDElementPosteBuilder {
    PointageDElementNatureBuilder poste(Optional<PosteDeTravailId> poste);
  }

  interface PointageDElementNatureBuilder {
    PointageDElementDateDeSurvenueBuilder nature(Optional<NatureDOperation> nature);
  }

  interface PointageDElementDateDeSurvenueBuilder {
    PointageDElement dateDeSurvenue(Instant dateDeSurvenue);
  }
}

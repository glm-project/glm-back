package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un pointage de l'operateur sur un element, tel qu'il figure dans le journal brut du jour : le geste, l'element, le
 * poste s'il a ete pointe, la nature figee a la saisie, et l'heure.
 */
public record PointageDElement(
  PointageId id,
  IntentionDePointage intention,
  Optional<ActiviteId> cible,
  TypeDEvenementDAtelier type,
  ElementId element,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  Instant dateDeSurvenue
) {
  public PointageDElement {
    Assert.notNull("id du pointage", id);
    Assert.notNull("intention", intention);
    Assert.notNull("cible", cible);
    Assert.notNull("type", type);
    Assert.notNull("element", element);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("date de survenue", dateDeSurvenue);
  }

  public static PointageDElementIdentiteBuilder builder() {
    return id ->
      intention ->
        cible ->
          type ->
            element ->
              poste -> nature -> dateDeSurvenue -> new PointageDElement(id, intention, cible, type, element, poste, nature, dateDeSurvenue);
  }

  public interface PointageDElementIdentiteBuilder {
    PointageDElementIntentionBuilder id(PointageId id);
  }

  public interface PointageDElementIntentionBuilder {
    PointageDElementCibleBuilder intention(IntentionDePointage intention);
  }

  public interface PointageDElementCibleBuilder {
    PointageDElementTypeBuilder cible(Optional<ActiviteId> cible);
  }

  public interface PointageDElementTypeBuilder {
    PointageDElementElementBuilder type(TypeDEvenementDAtelier type);
  }

  public interface PointageDElementElementBuilder {
    PointageDElementPosteBuilder element(ElementId element);
  }

  public interface PointageDElementPosteBuilder {
    PointageDElementNatureBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface PointageDElementNatureBuilder {
    PointageDElementDateDeSurvenueBuilder nature(Optional<NatureDOperation> nature);
  }

  public interface PointageDElementDateDeSurvenueBuilder {
    PointageDElement dateDeSurvenue(Instant dateDeSurvenue);
  }
}

package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Un pointage enregistre malgre tout, que le gestionnaire doit regarder (lot 8b de la strategie « bornes de fin de
 * journee ») : son temps compte deja. Il sort de la liste des qu'il est acquitte, annule ou corrige.
 *
 * <p>
 * L'horodatage est celui que l'evenement a retenu ; {@code dateDeclaree} garde la date du pupitre quand elle a ete
 * ramenee, pour que l'ecart reste lisible.
 * </p>
 */
public record PointageSignale(
  PointageSignaleId id,
  CibleDuSignalement cible,
  OperateurId operateur,
  Set<MotifDeSignalement> motifs,
  Horodatage horodatage,
  Optional<Instant> dateDeclaree,
  Optional<Resolution> resolution
) {
  public PointageSignale {
    Assert.notNull("id", id);
    Assert.notNull("cible", cible);
    Assert.notNull("operateur", operateur);
    Assert.field("motifs", motifs).notEmpty().noNullElement();
    motifs = Set.copyOf(motifs);
    Assert.notNull("horodatage", horodatage);
    Assert.notNull("date declaree", dateDeclaree);
    Assert.notNull("resolution", resolution);
  }

  public static PointageSignaleIdBuilder builder() {
    return id ->
      cible ->
        operateur ->
          motif ->
            horodatage ->
              dateDeclaree -> resolution -> new PointageSignale(id, cible, operateur, motif, horodatage, dateDeclaree, resolution);
  }

  /**
   * Le meme signalement, sorti de la liste. Il ne se resout qu'une fois : la premiere resolution est celle qui compte.
   */
  public PointageSignale resolu(Resolution nouvelle) {
    Assert.notNull("resolution", nouvelle);
    if (estResolu()) {
      throw new PointageSignaleDejaResoluException(id);
    }

    return new PointageSignale(id, cible, operateur, motifs, horodatage, dateDeclaree, Optional.of(nouvelle));
  }

  public boolean estResolu() {
    return resolution.isPresent();
  }

  public interface PointageSignaleIdBuilder {
    PointageSignaleCibleBuilder id(PointageSignaleId id);
  }

  public interface PointageSignaleCibleBuilder {
    PointageSignaleOperateurBuilder cible(CibleDuSignalement cible);
  }

  public interface PointageSignaleOperateurBuilder {
    PointageSignaleMotifsBuilder operateur(OperateurId operateur);
  }

  public interface PointageSignaleMotifsBuilder {
    PointageSignaleHorodatageBuilder motifs(Set<MotifDeSignalement> motifs);
  }

  public interface PointageSignaleHorodatageBuilder {
    PointageSignaleDateDeclareeBuilder horodatage(Horodatage horodatage);
  }

  public interface PointageSignaleDateDeclareeBuilder {
    PointageSignaleResolutionBuilder dateDeclaree(Optional<Instant> dateDeclaree);
  }

  public interface PointageSignaleResolutionBuilder {
    PointageSignale resolution(Optional<Resolution> resolution);
  }
}

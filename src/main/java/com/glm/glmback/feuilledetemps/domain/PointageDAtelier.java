package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un pointage de l'operateur sur un element, reduit a ce que la feuille de temps a besoin d'en savoir.
 *
 * <p>
 * Ni operateur, ni auteur, ni annulation : le journal est deja restreint a l'operateur lu, et les evenements annules
 * sont ecartes des la requete. Restent le geste, le poste qu'il engage et la nature que l'atelier a figee a la saisie.
 * </p>
 */
public record PointageDAtelier(
  TypeDEvenementDAtelier type,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  Instant dateDeSurvenue
) {
  public PointageDAtelier {
    Assert.notNull("type", type);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("date de survenue", dateDeSurvenue);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}.
   */
  public static PointageDAtelierTypeBuilder builder() {
    return type -> poste -> nature -> dateDeSurvenue -> new PointageDAtelier(type, poste, nature, dateDeSurvenue);
  }

  public interface PointageDAtelierTypeBuilder {
    PointageDAtelierPosteBuilder type(TypeDEvenementDAtelier type);
  }

  public interface PointageDAtelierPosteBuilder {
    PointageDAtelierNatureBuilder poste(Optional<PosteDeTravailId> poste);
  }

  public interface PointageDAtelierNatureBuilder {
    PointageDAtelierDateDeSurvenueBuilder nature(Optional<NatureDOperation> nature);
  }

  public interface PointageDAtelierDateDeSurvenueBuilder {
    PointageDAtelier dateDeSurvenue(Instant dateDeSurvenue);
  }
}

package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Un pointage de presence, reduit a ce qu'une feuille de temps a besoin d'en savoir.
 *
 * <p>
 * Ni auteur, ni date d'enregistrement, ni annulation : la correction est l'affaire de l'atelier, et les evenements
 * annules sont ecartes des la lecture. Ce qui reste est le seul fait qui compte ici — un type, a une heure.
 * </p>
 */
public record EvenementDePresence(TypeDEvenementDePresence type, Instant dateDeSurvenue) {
  public EvenementDePresence {
    Assert.notNull("type", type);
    Assert.notNull("date de survenue", dateDeSurvenue);
  }
}

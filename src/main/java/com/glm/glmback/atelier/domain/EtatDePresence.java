package com.glm.glmback.atelier.domain;

import java.util.Optional;

/**
 * Etat de presence d'un operateur dans l'entreprise, de son arrivee a son depart.
 *
 * <p>
 * La pause n'en est pas un : le pupitre la traduit en fins d'activite, et l'operateur en pause reste present.
 * </p>
 */
public enum EtatDePresence {
  ABSENT,
  PRESENT;

  public Optional<EtatDePresence> apres(TypeDEvenementDePresence type) {
    return switch (this) {
      case ABSENT -> depuisAbsent(type);
      case PRESENT -> depuisPresent(type);
    };
  }

  private static Optional<EtatDePresence> depuisAbsent(TypeDEvenementDePresence type) {
    return switch (type) {
      case ARRIVEE -> Optional.of(PRESENT);
      case DEPART -> Optional.empty();
    };
  }

  private static Optional<EtatDePresence> depuisPresent(TypeDEvenementDePresence type) {
    return switch (type) {
      case DEPART -> Optional.of(ABSENT);
      case ARRIVEE -> Optional.empty();
    };
  }
}

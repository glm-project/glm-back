package com.glm.glmback.coutderevient.domain;

import java.util.Optional;

/**
 * Etat de presence d'un operateur, rejoue depuis le journal d'atelier.
 *
 * <p>
 * Troisieme ecriture du meme automate, apres celle de l'atelier et celle de la feuille de temps. La duplication est le
 * prix de la frontiere : le partage passerait soit par un import interdit, soit par le shared kernel, qui est en
 * anglais et ne peut pas accueillir du vocabulaire d'atelier.
 * </p>
 */
public enum EtatDePresence {
  ABSENT,
  PRESENT,
  EN_PAUSE;

  public Optional<EtatDePresence> apres(TypeDEvenementDePresence type) {
    return switch (this) {
      case ABSENT -> depuisAbsent(type);
      case PRESENT -> depuisPresent(type);
      case EN_PAUSE -> depuisEnPause(type);
    };
  }

  private static Optional<EtatDePresence> depuisAbsent(TypeDEvenementDePresence type) {
    return switch (type) {
      case ARRIVEE -> Optional.of(PRESENT);
      case PAUSE, REPRISE, DEPART -> Optional.empty();
    };
  }

  private static Optional<EtatDePresence> depuisPresent(TypeDEvenementDePresence type) {
    return switch (type) {
      case PAUSE -> Optional.of(EN_PAUSE);
      case DEPART -> Optional.of(ABSENT);
      case ARRIVEE, REPRISE -> Optional.empty();
    };
  }

  private static Optional<EtatDePresence> depuisEnPause(TypeDEvenementDePresence type) {
    return switch (type) {
      case REPRISE -> Optional.of(PRESENT);
      case DEPART -> Optional.of(ABSENT);
      case ARRIVEE, PAUSE -> Optional.empty();
    };
  }
}

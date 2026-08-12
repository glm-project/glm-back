package com.glm.glmback.atelier.domain;

import java.util.Optional;

/**
 * Etat de presence d'un operateur dans l'entreprise.
 *
 * <p>
 * La pause suspend la presence sans y mettre fin : c'est ce qui distingue le temps ou l'operateur est payable a
 * travailler du temps ou il est simplement dans les murs. Le depart, lui, referme la journee depuis n'importe lequel
 * des deux etats ouverts, parce qu'un operateur peut partir sans avoir repris.
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

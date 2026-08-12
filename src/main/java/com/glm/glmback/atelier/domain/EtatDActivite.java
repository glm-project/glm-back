package com.glm.glmback.atelier.domain;

import java.util.Optional;

/**
 * Etat d'une activite, c'est a dire du couple (operateur, poste de travail) sur un element engage.
 *
 * <p>
 * La reprise apres une non conformite se pointe comme un debut : les deux categories se distinguent a la lecture, sur
 * l'etat atteint, jamais sur un troisieme type d'evenement. Une pause ne figure pas ici, elle suspend la presence de
 * l'operateur et non son activite sur l'element.
 * </p>
 */
public enum EtatDActivite {
  ABSENTE,
  EN_COURS,
  EN_NON_CONFORMITE;

  public Optional<EtatDActivite> apres(TypeDEvenementDAtelier type) {
    return switch (this) {
      case ABSENTE -> depuisAbsente(type);
      case EN_COURS -> depuisEnCours(type);
      case EN_NON_CONFORMITE -> depuisEnNonConformite(type);
    };
  }

  public Optional<CategorieDActivite> categorie() {
    return switch (this) {
      case ABSENTE -> Optional.empty();
      case EN_COURS -> Optional.of(CategorieDActivite.TRAVAIL);
      case EN_NON_CONFORMITE -> Optional.of(CategorieDActivite.NON_CONFORMITE);
    };
  }

  private static Optional<EtatDActivite> depuisAbsente(TypeDEvenementDAtelier type) {
    return switch (type) {
      case DEBUT -> Optional.of(EN_COURS);
      case NON_CONFORMITE -> Optional.of(EN_NON_CONFORMITE);
      case FIN -> Optional.empty();
    };
  }

  private static Optional<EtatDActivite> depuisEnCours(TypeDEvenementDAtelier type) {
    return switch (type) {
      case NON_CONFORMITE -> Optional.of(EN_NON_CONFORMITE);
      case FIN -> Optional.of(ABSENTE);
      case DEBUT -> Optional.empty();
    };
  }

  private static Optional<EtatDActivite> depuisEnNonConformite(TypeDEvenementDAtelier type) {
    return switch (type) {
      case DEBUT -> Optional.of(EN_COURS);
      case FIN -> Optional.of(ABSENTE);
      case NON_CONFORMITE -> Optional.empty();
    };
  }
}

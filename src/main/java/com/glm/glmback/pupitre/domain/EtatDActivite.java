package com.glm.glmback.pupitre.domain;

import java.util.Optional;

/**
 * Etat d'une activite, c'est a dire du couple (operateur, poste de travail) sur un element engage.
 *
 * <p>
 * La reprise apres une non conformite se pointe comme un debut : les deux categories se distinguent sur l'etat
 * atteint, jamais sur un troisieme type d'evenement. C'est le meme automate que celui de l'atelier, rejoue ici parce
 * que le contexte voisin est {@code BusinessContext} et ne s'importe pas.
 * </p>
 */
public enum EtatDActivite {
  ABSENTE,
  EN_COURS,
  EN_NON_CONFORMITE;

  public Optional<EtatDActivite> apres(TypeDePointage type) {
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

  private static Optional<EtatDActivite> depuisAbsente(TypeDePointage type) {
    return switch (type) {
      case DEBUT -> Optional.of(EN_COURS);
      case NON_CONFORMITE -> Optional.of(EN_NON_CONFORMITE);
      case FIN -> Optional.empty();
    };
  }

  private static Optional<EtatDActivite> depuisEnCours(TypeDePointage type) {
    return switch (type) {
      case NON_CONFORMITE -> Optional.of(EN_NON_CONFORMITE);
      case FIN -> Optional.of(ABSENTE);
      case DEBUT -> Optional.empty();
    };
  }

  private static Optional<EtatDActivite> depuisEnNonConformite(TypeDePointage type) {
    return switch (type) {
      case DEBUT -> Optional.of(EN_COURS);
      case FIN -> Optional.of(ABSENTE);
      case NON_CONFORMITE -> Optional.empty();
    };
  }
}

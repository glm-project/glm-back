package com.glm.glmback.coutderevient.domain;

import java.util.Optional;

/**
 * Etat d'une activite — un operateur sur un poste de travail —, rejoue depuis le journal d'atelier.
 *
 * <p>
 * Meme automate que celui de l'atelier, redeclare ici : ce contexte lit les memes tables sans importer le paquet
 * voisin, annote {@code BusinessContext}. La categorie se lit sur l'etat atteint, jamais sur le type d'evenement,
 * ce qui fait qu'une reprise de bon travail apres une non conformite se pointe comme un debut.
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

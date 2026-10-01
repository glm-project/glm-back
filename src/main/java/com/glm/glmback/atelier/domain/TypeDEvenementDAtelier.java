package com.glm.glmback.atelier.domain;

import java.util.Optional;

/**
 * Ce qu'un pointage dit d'une activite sur un element.
 *
 * <p>
 * Aucun type propre a la pause : le pupitre la pointe par une fin sur chaque activite en cours, et la reprise par une
 * ouverture en debut, ou en non conformite pour l'activite qui en etait une. Le type ne dit pas non plus s'il ouvre ou
 * remplace une activite : c'est l'{@link IntentionDePointage} qui le dit.
 * </p>
 */
public enum TypeDEvenementDAtelier {
  DEBUT,
  NON_CONFORMITE,
  FIN;

  /**
   * La categorie de l'activite qu'ouvre un pointage de ce type : aucune pour une fin.
   */
  public Optional<CategorieDActivite> categorie() {
    return switch (this) {
      case DEBUT -> Optional.of(CategorieDActivite.TRAVAIL);
      case NON_CONFORMITE -> Optional.of(CategorieDActivite.NON_CONFORMITE);
      case FIN -> Optional.empty();
    };
  }
}

package com.glm.glmback.atelier.domain;

import java.util.Optional;

/**
 * Ce qu'un fait du journal dit d'une activite sur un element.
 *
 * <p>
 * Trois gestes d'activite : un debut et une non conformite ouvrent une activite ; une fin ferme celle qui est en cours
 * sur la cle. Aucun type propre a la pause : le pupitre la pointe par une fin sur chaque activite en cours, et la
 * reprise par une ouverture en debut, ou en non conformite pour l'activite qui en etait une.
 * </p>
 *
 * <p>
 * Deux faits du gestionnaire s'y ajoutent, qui visent chacun une activite et n'en ouvrent aucune : un retrait l'ecarte de
 * toutes les lectures, un changement de poste la fait compter sur un autre poste. Dates du moment de la correction, ce ne
 * sont pas des gestes : la regle de reception ne les compte jamais. (Ajouter une activite n'a pas de type propre : c'est un
 * debut et une fin.)
 * </p>
 */
public enum TypeDEvenementDAtelier {
  DEBUT,
  NON_CONFORMITE,
  FIN,
  RETRAIT,
  CHANGEMENT_DE_POSTE;

  /**
   * Vrai pour un debut et une non conformite, qui ouvrent une activite ; faux pour une fin, qui ferme celle qui est en
   * cours sur la cle, et pour une correction.
   */
  public boolean ouvreUneActivite() {
    return this == DEBUT || this == NON_CONFORMITE;
  }

  /**
   * Vrai pour un debut, une non conformite et une fin : ce que le pupitre pointe et ce que la regle de reception juge.
   * Faux pour un retrait et un changement de poste, faits du gestionnaire qui visent une activite sans la borner.
   */
  public boolean estUnGeste() {
    return this == DEBUT || this == NON_CONFORMITE || this == FIN;
  }

  /**
   * La categorie de l'activite qu'ouvre un pointage de ce type : aucune pour une fin.
   */
  public Optional<CategorieDActivite> categorie() {
    return switch (this) {
      case DEBUT -> Optional.of(CategorieDActivite.TRAVAIL);
      case NON_CONFORMITE -> Optional.of(CategorieDActivite.NON_CONFORMITE);
      case FIN, RETRAIT, CHANGEMENT_DE_POSTE -> Optional.empty();
    };
  }
}

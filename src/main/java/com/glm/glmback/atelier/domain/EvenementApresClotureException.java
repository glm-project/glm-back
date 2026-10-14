package com.glm.glmback.atelier.domain;

/**
 * Un fait que le gestionnaire ajoute au journal d'un suivi cloture, date apres la cloture : la fin d'une activite
 * ajoutee, un retrait ou un changement de poste, dates du moment de la correction. Symetrique de
 * {@link EvenementAvantEngagementException}. Le pointage et le deplacement de la cloture gardent leur propre refus.
 */
public final class EvenementApresClotureException extends RuntimeException {

  public EvenementApresClotureException(EvenementDAtelier evenement) {
    super("L'evenement %s du %s est posterieur a la cloture du suivi".formatted(evenement.type(), evenement.dateDeSurvenue()));
  }
}

package com.glm.glmback.atelier.domain;

/**
 * Un pointage dont le type, l'intention et les activites qu'il ouvre ou vise ne s'accordent pas. Le contrat HTTP le
 * refuse avant qu'il n'atteigne le domaine : c'est un defaut, jamais un cas d'usage.
 */
public final class IntentionDePointageIncoherenteException extends RuntimeException {

  public IntentionDePointageIncoherenteException(TypeDEvenementDAtelier type, IntentionDePointage intention) {
    super(
      (
        "Le pointage %s d'intention %s est incoherent : seule une fin se pointe FIN, une ouverture ou une transition ouvre "
        + "une activite, une transition ou une fin en vise une"
      ).formatted(type, intention)
    );
  }
}

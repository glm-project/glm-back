package com.glm.glmback.atelier.domain.gestionanomalies;

public final class NatureDAnomalieInvalideException extends RuntimeException {

  public NatureDAnomalieInvalideException(String valeur) {
    super(message(valeur));
  }

  private static String message(String valeur) {
    String possibles = "Valeurs possibles : " + String.join(", ", NatureDAnomalie.noms()) + ".";
    return valeur == null
      ? "La nature d'anomalie est obligatoire. " + possibles
      : "La nature d'anomalie '" + valeur + "' est inconnue. " + possibles;
  }
}

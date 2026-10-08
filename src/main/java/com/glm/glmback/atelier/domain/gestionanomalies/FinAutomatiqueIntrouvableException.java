package com.glm.glmback.atelier.domain.gestionanomalies;

public final class FinAutomatiqueIntrouvableException extends RuntimeException {

  public FinAutomatiqueIntrouvableException(AdresseDossierAnomalie adresse) {
    super(
      "Le pointage %s du suivi %s n'ouvre aucune fin automatique a regulariser".formatted(adresse.pointage().uuid(), adresse.suivi().uuid())
    );
  }
}

package com.glm.glmback.atelier.domain.gestionanomalies;

import java.util.UUID;

public final class ConfirmationReutiliseeException extends RuntimeException {

  public ConfirmationReutiliseeException(UUID commande) {
    super("La confirmation %s est deja associee a un acte ou un gestionnaire different".formatted(commande));
  }
}

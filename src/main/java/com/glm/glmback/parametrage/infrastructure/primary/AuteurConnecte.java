package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.domain.Auteur;
import com.glm.glmback.shared.authentication.application.AuthenticatedUser;

/**
 * L'auteur d'une modification est toujours l'utilisateur du jeton, jamais une valeur du corps de la requete.
 */
final class AuteurConnecte {

  private AuteurConnecte() {}

  static Auteur get() {
    return new Auteur(AuthenticatedUser.username().get());
  }
}

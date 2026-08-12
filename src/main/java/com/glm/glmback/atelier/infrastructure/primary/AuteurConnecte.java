package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.shared.authentication.application.AuthenticatedUser;

/**
 * L'auteur d'une saisie est toujours l'utilisateur du jeton, jamais une valeur du corps de la requete.
 *
 * <p>
 * C'est ce qui rend la tracabilite infalsifiable, et ce qui donne son sens a l'invariant du contexte : une
 * regularisation se reconnait a l'ecart de ses deux dates, jamais a l'identite de son auteur.
 * </p>
 */
final class AuteurConnecte {

  private AuteurConnecte() {}

  static Auteur get() {
    return new Auteur(AuthenticatedUser.username().get());
  }
}

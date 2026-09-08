package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce dont on parle quand on parle d'une activite : un operateur sur un poste de travail.
 *
 * <p>
 * C'est le poste, et non la nature de l'operation, qui distingue deux activites menees de front — et c'est pour la
 * meme raison que le diviseur du taux horaire compte des postes et non des elements. Un operateur sur trois elements
 * avec une seule machine ne mene qu'une activite, et son heure ne se divise pas.
 * </p>
 */
public record CleDActivite(OperateurId operateur, Optional<PosteDeTravailId> poste) {
  public CleDActivite {
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
  }
}

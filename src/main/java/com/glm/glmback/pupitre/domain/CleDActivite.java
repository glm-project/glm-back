package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce dont on parle quand on parle d'une activite : un operateur sur un poste de travail.
 *
 * <p>
 * L'automate se joue par cle : c'est le poste, et non la nature de l'operation, qui distingue deux activites menees
 * de front. Le poste reste facultatif, pour l'entreprise qui n'a aucun parc machine.
 * </p>
 */
public record CleDActivite(OperateurId operateur, Optional<PosteDeTravailId> poste) {
  public CleDActivite {
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
  }
}

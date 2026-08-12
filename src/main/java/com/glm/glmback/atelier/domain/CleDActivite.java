package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce dont on parle quand on parle d'une activite : un operateur sur un poste de travail.
 *
 * <p>
 * L'automate se joue par cle : c'est le poste, et non la nature de l'operation, qui distingue deux activites menees de
 * front. Sans quoi l'operateur qui met deux pieces du meme element sur deux machines se heurterait a lui-meme, alors
 * que le client demande explicitement ce cas.
 * </p>
 */
public record CleDActivite(Operateur operateur, Optional<PosteDeTravail> poste) {
  public CleDActivite {
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
  }
}

package com.glm.glmback.atelier.application.gestionconflits;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.shared.error.domain.Assert;

/** L'identite authentifiee est stable meme si le nom d'auteur est modifie. */
public record IdentiteDuGestionnaire(Auteur auteur, String sujet, String emetteur) {
  public IdentiteDuGestionnaire {
    Assert.notNull("auteur", auteur);
    Assert.notBlank("sujet", sujet);
    Assert.notBlank("emetteur", emetteur);
  }

  public boolean estLaMemePersonneQue(IdentiteDuGestionnaire autre) {
    return sujet.equals(autre.sujet) && emetteur.equals(autre.emetteur);
  }
}

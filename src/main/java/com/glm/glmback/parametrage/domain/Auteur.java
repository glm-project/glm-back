package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * L'utilisateur qui a modifie le parametrage, lu dans son jeton et jamais dans le corps de la requete.
 */
public record Auteur(String value) {
  public Auteur {
    Assert.notBlank("auteur", value);
  }
}

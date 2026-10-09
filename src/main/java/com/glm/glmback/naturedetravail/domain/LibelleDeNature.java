package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le nom d'une nature tel que le gestionnaire l'a saisi, sans les espaces qui l'entourent : c'est lui qui s'affiche
 * partout, rapports passes compris.
 */
public record LibelleDeNature(String value) {
  private static final int MAX_LENGTH = 50;

  public LibelleDeNature {
    Assert.field("libelle", value).notBlank();
    value = value.strip();
    Assert.field("libelle", value).maxLength(MAX_LENGTH);
  }

  /**
   * Deux libelles qui ne different que par la casse, les accents ou les espaces designent la meme nature.
   */
  public CleDeNature cle() {
    return CleDeNature.de(this);
  }
}

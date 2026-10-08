package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Une categorie telle que la liste la montre : avec, en plus, le fait que des produits y soient deja ranges, ce qui
 * interdit de la supprimer.
 */
public record CategorieDeProduitListee(CategorieDeProduit categorie, boolean utilisee) {
  public CategorieDeProduitListee {
    Assert.notNull("categorie", categorie);
  }
}

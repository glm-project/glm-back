package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce que le decodage d'un fichier en dit : son format, tel que le decodeur le nomme, et ses dimensions en pixels.
 */
public record ImageLue(String format, int largeur, int hauteur) {
  public ImageLue {
    Assert.notBlank("format", format);
    Assert.field("largeur", largeur).positive();
    Assert.field("hauteur", hauteur).positive();
  }
}

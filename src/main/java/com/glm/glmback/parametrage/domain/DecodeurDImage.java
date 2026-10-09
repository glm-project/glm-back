package com.glm.glmback.parametrage.domain;

import java.util.Optional;

public interface DecodeurDImage {
  /**
   * Vide si le contenu n'est pas une image lisible.
   */
  Optional<ImageLue> lis(byte[] contenu);
}

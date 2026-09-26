package com.glm.glmback.atelier.application;

import java.util.Optional;

/**
 * Ce qu'un geste du pupitre a produit : l'agregat qui l'a recu, s'il a ete rejoue ou absorbe, ou rien quand il a ete
 * mis en attente faute de pouvoir le rattacher.
 */
public record ResultatDEcriture<T>(Optional<T> agregat, boolean rejeu) {
  public ResultatDEcriture(T agregat, boolean rejeu) {
    this(Optional.of(agregat), rejeu);
  }

  public static <T> ResultatDEcriture<T> enAttente(boolean rejeu) {
    return new ResultatDEcriture<>(Optional.empty(), rejeu);
  }
}

package com.glm.glmback.atelier.domain;

/**
 * L'issue d'un geste du pupitre : enregistre (ou absorbe, ou rejoue), ou mis en attente faute de pouvoir le
 * rattacher.
 */
public sealed interface Recueil<T> {
  record Enregistre<T>(T valeur) implements Recueil<T> {}

  record MisEnAttente<T>(PointageEnAttente pointage) implements Recueil<T> {}
}

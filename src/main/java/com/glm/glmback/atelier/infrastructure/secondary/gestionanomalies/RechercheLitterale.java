package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

/** Un texte cherche tel quel, en sous-chaine : pourcent, soulignement et antislash ne sont jamais des jokers. */
final class RechercheLitterale {

  private RechercheLitterale() {}

  static String motif(String texte) {
    return "%" + texte.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}

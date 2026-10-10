package com.glm.glmback.coutderevient.domain;

/** Le rapport porte une fin automatique non regularisee ou un tarif manquant : il ne part pas chez le client. */
public final class RapportNonExportableException extends RuntimeException {

  public RapportNonExportableException(CoutDeRevient rapport) {
    super(
      "Le cout de revient de %s ne peut pas etre exporte : %d fin(s) automatique(s) a regulariser, %d pointage(s) sans tarif".formatted(
        rapport.element().nom().value(),
        rapport.finsAutomatiques(),
        rapport.tarifsManquants()
      )
    );
  }
}

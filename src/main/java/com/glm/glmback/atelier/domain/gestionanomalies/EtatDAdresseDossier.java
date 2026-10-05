package com.glm.glmback.atelier.domain.gestionanomalies;

public enum EtatDAdresseDossier {
  EN_CONFLIT,
  INTROUVABLE,
  ANCRE_ANNULEE,
  FIN_AUTOMATIQUE,
  SANS_ANOMALIE;

  /** Vrai si l'adresse ouvre une anomalie que le gestionnaire peut traiter par un apercu puis une confirmation. */
  public boolean estATraiter() {
    return this == EN_CONFLIT || this == FIN_AUTOMATIQUE;
  }
}

package com.glm.glmback.coutderevient.domain;

/** Prepare le compte rendu d'un element, a partir du rapport que l'ecran lit. */
public final class ComptesRendusDuCout {

  private final CoutsDeRevientService coutsDeRevient;
  private final FuseauHoraireDeLEntreprise fuseau;

  public ComptesRendusDuCout(CoutsDeRevientService coutsDeRevient, FuseauHoraireDeLEntreprise fuseau) {
    this.coutsDeRevient = coutsDeRevient;
    this.fuseau = fuseau;
  }

  public CompteRenduDuCout compteRendu(ElementId element) {
    return new CompteRenduDuCout(coutsDeRevient.rapport(element), fuseau.zone());
  }
}

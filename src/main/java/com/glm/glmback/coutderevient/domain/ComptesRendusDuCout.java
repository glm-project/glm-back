package com.glm.glmback.coutderevient.domain;

/** Prepare le compte rendu d'un element, a partir du rapport que l'ecran lit. */
public final class ComptesRendusDuCout {

  private final CoutsDeRevientService coutsDeRevient;
  private final PassagesEnAtelier passages;
  private final FuseauHoraireDeLEntreprise fuseau;

  public ComptesRendusDuCout(CoutsDeRevientService coutsDeRevient, PassagesEnAtelier passages, FuseauHoraireDeLEntreprise fuseau) {
    this.coutsDeRevient = coutsDeRevient;
    this.passages = passages;
    this.fuseau = fuseau;
  }

  public CompteRenduDuCout compteRendu(ElementId element) {
    CoutDeRevient rapport = coutsDeRevient.rapport(element);
    return new CompteRenduDuCout(rapport, StatutDeLElement.de(passages.passages(element), rapport.lecture()), fuseau.zone());
  }
}

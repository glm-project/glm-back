package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;
import java.util.List;

/**
 * Le travail en memoire : il rend les suivis qu'on lui a declares, et retient les bornes demandees.
 *
 * <p>
 * Aucun filtrage ici — c'est la requete SQL de l'adapter qui choisit les suivis, et la reduction a la presence qui
 * ecarte ce qui tombe hors des journees.
 * </p>
 */
final class TravailEnMemoire implements TravailDeLOperateur {

  private final List<SuiviDuTravail> suivis;
  private Instant depuisDemande;
  private Instant finExclusiveDemandee;

  private TravailEnMemoire(List<SuiviDuTravail> suivis) {
    this.suivis = suivis;
  }

  static TravailEnMemoire sansSuivi() {
    return new TravailEnMemoire(List.of());
  }

  static TravailEnMemoire avec(List<SuiviDuTravail> suivis) {
    return new TravailEnMemoire(suivis);
  }

  @Override
  public List<SuiviDuTravail> suivis(OperateurId operateur, Instant depuis, Instant finExclusive) {
    depuisDemande = depuis;
    finExclusiveDemandee = finExclusive;

    return suivis;
  }

  Instant depuisDemande() {
    return depuisDemande;
  }

  Instant finExclusiveDemandee() {
    return finExclusiveDemandee;
  }
}

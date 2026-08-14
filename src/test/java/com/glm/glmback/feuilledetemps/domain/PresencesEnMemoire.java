package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;
import java.util.List;

/**
 * La presence en memoire : elle rend les journees qu'on lui a declarees, et retient les bornes demandees.
 *
 * <p>
 * Aucun filtrage ici — c'est la requete SQL de l'adapter qui borne, et le decoupage calendaire qui coupe. Le double
 * sert justement a verifier que le service demande les bonnes bornes, et qu'une journee hors semaine ne produit rien
 * meme si le port la rend.
 * </p>
 */
final class PresencesEnMemoire implements PresenceDeLOperateur {

  private final List<JourneeDeTravail> journees;
  private Instant debutDemande;
  private Instant finExclusiveDemandee;

  private PresencesEnMemoire(List<JourneeDeTravail> journees) {
    this.journees = journees;
  }

  static PresencesEnMemoire sansJournee() {
    return new PresencesEnMemoire(List.of());
  }

  static PresencesEnMemoire avec(List<JourneeDeTravail> journees) {
    return new PresencesEnMemoire(journees);
  }

  @Override
  public List<JourneeDeTravail> journeesRecouvrant(OperateurId operateur, Instant debut, Instant finExclusive) {
    debutDemande = debut;
    finExclusiveDemandee = finExclusive;

    return journees;
  }

  Instant debutDemande() {
    return debutDemande;
  }

  Instant finExclusiveDemandee() {
    return finExclusiveDemandee;
  }
}

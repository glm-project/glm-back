package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/**
 * Une tranche d'activite reduite a une fenetre de partage : tout ce qu'il faut pour chiffrer sa main d'oeuvre.
 *
 * <p>
 * Le diviseur ne concerne que la main d'oeuvre. La machine, jamais partagee, se chiffre sur la tranche entiere
 * ({@link TrancheDActivite#coutMachine()}) : elle n'a rien a faire d'un decoupage qui ne la concerne pas.
 * </p>
 */
public record TrancheValorisable(TrancheDActivite tranche, FenetreDePartage fenetre) {
  public TrancheValorisable {
    Assert.notNull("tranche", tranche);
    Assert.notNull("fenetre", fenetre);
  }

  public Diviseur diviseur() {
    return fenetre.diviseur();
  }

  /**
   * Ce que l'operateur menait d'autre pendant cette part, tous elements confondus : de quoi justifier son diviseur.
   */
  public List<Activite> paralleles() {
    return fenetre
      .occupation()
      .stream()
      .filter(autre -> autre.periode().intersection(tranche.periode()).isPresent())
      .filter(autre -> !autre.reduiteA(fenetre.periode()).equals(Optional.of(tranche)))
      .map(TrancheDActivite::activite)
      .distinct()
      .toList();
  }

  /**
   * Ce que la personne a coute pendant cette part, deja arrondi au centime par la repartition de sa fenetre : elle
   * ne peut pas etre payee deux fois la meme heure. Zero quand l'operateur n'est pas valorise, quel que soit le
   * diviseur.
   */
  public Montant coutDeMainDOeuvre() {
    return fenetre.repartition().de(tranche);
  }
}

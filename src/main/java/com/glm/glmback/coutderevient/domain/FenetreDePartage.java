package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

/**
 * Une periode maximale pendant laquelle un operateur occupe le meme ensemble de postes : l'unite d'arrondi de sa
 * main d'oeuvre.
 *
 * <p>
 * Les sous-periodes se coupent a chaque debut et a chaque fin de tranche, meme quand l'ensemble des postes ne change
 * pas. Arrondir chacune ferait deriver une heure qui n'est pas partagee : la fenetre les reunit tant que cet ensemble
 * reste le meme.
 * </p>
 */
public record FenetreDePartage(SousPeriode etendue, RepartitionDeMainDOeuvre repartition, List<TrancheDActivite> occupation) {
  /**
   * L'occupation garde les tranches de l'operateur qui recouvrent la fenetre, tous elements confondus : ce sont elles
   * qui expliquent un partage, aussi sur tel poste pour tel element.
   */
  public FenetreDePartage {
    Assert.notNull("etendue", etendue);
    Assert.notNull("repartition", repartition);
    Assert.field("occupation", occupation).notNull().noNullElement();
    occupation = List.copyOf(occupation);
  }

  public Periode periode() {
    return etendue.periode();
  }

  public Diviseur diviseur() {
    return etendue.diviseur();
  }
}

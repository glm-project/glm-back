package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import java.util.Set;

/**
 * Une periode maximale pendant laquelle un operateur occupe le meme ensemble de postes : l'unite d'arrondi de sa
 * main d'oeuvre.
 *
 * <p>
 * Les sous-periodes se coupent a chaque debut et a chaque fin de tranche, meme quand l'ensemble des postes ne change
 * pas. Arrondir chacune ferait deriver une heure qui n'est pas partagee : la fenetre les reunit tant que cet ensemble
 * reste le meme et que le diviseur est connu. Une sous-periode incertaine reste seule, sans repartition.
 * </p>
 */
public record FenetreDePartage(SousPeriode etendue, RepartitionDeMainDOeuvre repartition) {
  public FenetreDePartage {
    Assert.notNull("etendue", etendue);
    Assert.notNull("repartition", repartition);
  }

  public Periode periode() {
    return etendue.periode();
  }

  public Optional<Diviseur> diviseur() {
    return etendue.diviseur();
  }

  public Set<ActiviteInterpretee> responsables() {
    return etendue.responsables();
  }
}

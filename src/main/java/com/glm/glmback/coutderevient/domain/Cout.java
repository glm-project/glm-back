package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Un cout, separe en ce que coute la machine et en ce que coute la personne.
 *
 * <p>
 * Les deux ne s'agregent pas de la meme facon, et c'est pourquoi ils restent distincts jusqu'au bout : le cout de
 * chaque machine active court en entier, le taux de l'operateur se divise par le nombre de machines qu'il occupe.
 * Confondre les deux rendrait la regle invisible sur l'ecran qui l'affiche.
 * </p>
 */
public record Cout(Montant machine, Montant mainDOeuvre) {
  public static final Cout AUCUN = new Cout(Montant.ZERO, Montant.ZERO);

  public Cout {
    Assert.notNull("cout machine", machine);
    Assert.notNull("cout de main d'oeuvre", mainDOeuvre);
  }

  public Montant total() {
    return machine.plus(mainDOeuvre);
  }

  public Cout plus(Cout autre) {
    return new Cout(machine.plus(autre.machine), mainDOeuvre.plus(autre.mainDOeuvre));
  }
}

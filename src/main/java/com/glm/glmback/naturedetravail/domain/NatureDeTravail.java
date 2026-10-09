package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Un metier exerce dans l'atelier : soudage, tournage, fraisage, dessin.
 *
 * <p>
 * L'entreprise les declare une fois, puis les postes les choisissent dans cette liste : une faute de frappe ne cree
 * plus un metier de plus.
 * </p>
 */
public record NatureDeTravail(NatureDeTravailId id, LibelleDeNature libelle) {
  public NatureDeTravail {
    Assert.notNull("id", id);
    Assert.notNull("libelle", libelle);
  }

  /**
   * Seule la ligne de la nature change : ceux qui s'en servent n'en retiennent que l'identifiant, et liront le nouveau
   * libelle partout, rapports passes compris.
   */
  public NatureDeTravail renomme(LibelleDeNature libelle) {
    return new NatureDeTravail(id, libelle);
  }
}

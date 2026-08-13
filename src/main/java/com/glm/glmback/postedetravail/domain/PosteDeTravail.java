package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce sur quoi un operateur pointe : une machine, un etabli, un four, une salle.
 *
 * <p>
 * Le terme est volontairement generique, comme dans l'atelier. La nature, en revanche, est obligatoire ici : un poste
 * n'est declare que pour dire quel travail s'y fait.
 * </p>
 */
public record PosteDeTravail(PosteDeTravailId id, Libelle libelle, NatureDeTravail nature) {
  public PosteDeTravail {
    Assert.notNull("id", id);
    Assert.notNull("libelle", libelle);
    Assert.notNull("nature de travail", nature);
  }

  public PosteDeTravail revise(Libelle libelle, NatureDeTravail nature) {
    return new PosteDeTravail(id, libelle, nature);
  }
}

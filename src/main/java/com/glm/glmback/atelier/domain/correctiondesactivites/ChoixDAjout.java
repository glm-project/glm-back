package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

/**
 * Ce que le gestionnaire peut choisir pour ajouter une activite ce jour-la : les suivis presents, un par suivi, et les
 * postes sur lesquels l'operateur est habilite, avec leur nature. Les habilitations n'ont aucune date : ce sont les
 * habilitations actuelles.
 */
public record ChoixDAjout(List<SuiviPresent> suivis, List<PosteConnu> postes) {
  public ChoixDAjout {
    Assert.notNull("suivis", suivis);
    Assert.notNull("postes", postes);

    suivis = List.copyOf(suivis);
    postes = List.copyOf(postes);
  }
}

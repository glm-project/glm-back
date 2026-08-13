package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Comparator;
import java.util.List;

/**
 * Un operateur et les postes auxquels il est habilite, resolus.
 *
 * <p>
 * C'est ici que le metier se deduit : {@link #natures()} rend les natures de ses postes, sans qu'aucune n'ait ete
 * saisie sur la personne. La deduction vit dans le domaine, jamais dans un controleur.
 * </p>
 *
 * <p>
 * Les postes sont ordonnes par libelle des la construction : deux lectures identiques doivent rendre la meme reponse,
 * or l'ordre d'iteration de l'ensemble d'identifiants porte par l'operateur n'est pas specifie.
 * </p>
 */
public record ProfilDOperateur(Operateur operateur, List<PosteHabilitable> postes) {
  public ProfilDOperateur {
    Assert.notNull("operateur", operateur);
    Assert.field("postes", postes).notNull().noNullElement();

    postes = postes.stream().sorted(Comparator.comparing(PosteHabilitable::libelle)).toList();
  }

  public List<NatureDeTravail> natures() {
    return postes.stream().map(PosteHabilitable::nature).distinct().sorted().toList();
  }
}

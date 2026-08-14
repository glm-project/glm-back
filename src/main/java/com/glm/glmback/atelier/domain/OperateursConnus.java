package com.glm.glmback.atelier.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ce que l'atelier sait des operateurs, sans jamais dependre de leur contexte.
 *
 * <p>
 * L'ecriture de la presence n'a besoin que de l'existence : le journal ne stocke qu'un identifiant, il n'a rien a
 * copier. L'ecriture du journal d'atelier, elle, a besoin de la fiche entiere pour y recopier le taux horaire au
 * moment de la saisie, sur le meme patron que {@code PostesConnus.get}. La lecture d'une page, elle, resout un
 * journal entier en une requete plutot qu'une par evenement.
 * </p>
 */
public interface OperateursConnus {
  boolean existe(OperateurId id);

  Optional<OperateurConnu> get(OperateurId id);

  List<OperateurConnu> parIds(Set<OperateurId> ids);
}

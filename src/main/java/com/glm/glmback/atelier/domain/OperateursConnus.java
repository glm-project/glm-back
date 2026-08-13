package com.glm.glmback.atelier.domain;

import java.util.List;
import java.util.Set;

/**
 * Ce que l'atelier sait des operateurs, sans jamais dependre de leur contexte.
 *
 * <p>
 * L'ecriture n'a besoin que de l'existence : le journal ne stocke qu'un identifiant, il n'a rien a copier. La lecture,
 * elle, resout un journal entier en une requete plutot qu'une par evenement.
 * </p>
 */
public interface OperateursConnus {
  boolean existe(OperateurId id);

  List<OperateurConnu> parIds(Set<OperateurId> ids);
}

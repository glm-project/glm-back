package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Regles de selection des operateurs.
 *
 * <p>
 * Le filtre par poste repond a la question dont le pupitre a besoin : qui est habilite sur ce poste ? Absent, il ne
 * filtre rien.
 * </p>
 */
public record OperateurCriteria(Optional<PosteHabilitableId> poste) {
  public OperateurCriteria {
    Assert.notNull("poste", poste);
  }

  public boolean matches(Operateur operateur) {
    return poste.map(attendu -> operateur.postes().contains(attendu)).orElse(true);
  }
}

package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le suivi apres une correction, et si celle-ci n'etait qu'un renvoi : l'identifiant de la saisie figurait deja au
 * journal, rien n'a ete ecrit.
 */
public record CorrectionTraitee(SuiviDAtelier suivi, boolean rejeu) {
  public CorrectionTraitee {
    Assert.notNull("suivi", suivi);
  }
}

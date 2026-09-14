package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite du suivi d'atelier, celle que portent les URLs de pointage.
 *
 * <p>
 * Ce n'est pas l'identifiant de l'element de fabrication : le pupitre pointe sur le suivi, jamais sur l'element.
 * </p>
 */
public record SuiviDuPupitreId(UUID uuid) {
  public SuiviDuPupitreId {
    Assert.notNull("id du suivi", uuid);
  }
}

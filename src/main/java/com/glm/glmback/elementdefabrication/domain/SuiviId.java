package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite du suivi d'atelier d'un element : ce contexte la lit sans connaitre l'atelier, pour que l'ecran mene a
 * ses actes.
 */
public record SuiviId(UUID uuid) {
  public SuiviId {
    Assert.notNull("suivi", uuid);
  }
}

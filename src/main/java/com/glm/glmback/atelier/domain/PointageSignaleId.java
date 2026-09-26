package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * Un pointage signale porte l'identite de l'evenement qu'il signale : un evenement l'est au plus une fois, tous motifs
 * reunis.
 */
public record PointageSignaleId(UUID uuid) {
  public PointageSignaleId {
    Assert.notNull("id du pointage signale", uuid);
  }
}

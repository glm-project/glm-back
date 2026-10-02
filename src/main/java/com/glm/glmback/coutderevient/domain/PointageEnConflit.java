package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.UUID;

/**
 * Un fait actif d'une sequence en conflit, tel que le detail le montre : ce qu'il disait et quand. C'est ce qui
 * explique qu'un pointage reste a resoudre, « deux debuts a 08:00 et 09:10, sans fin entre les deux ».
 */
public record PointageEnConflit(UUID evenement, TypeDePointage type, Instant survenue) {
  public PointageEnConflit {
    Assert.notNull("evenement", evenement);
    Assert.notNull("type du pointage", type);
    Assert.notNull("date de survenue", survenue);
  }
}

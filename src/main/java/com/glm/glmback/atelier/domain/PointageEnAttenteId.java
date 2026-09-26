package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * Attribue par le serveur : un geste mis en attente pour un identifiant reutilise ne peut pas reprendre celui du
 * pupitre, deja pris par un autre contenu.
 */
public record PointageEnAttenteId(UUID uuid) implements Comparable<PointageEnAttenteId> {
  public PointageEnAttenteId {
    Assert.notNull("id du pointage en attente", uuid);
  }

  public static PointageEnAttenteId newId() {
    return new PointageEnAttenteId(UUID.randomUUID());
  }

  @Override
  public int compareTo(PointageEnAttenteId autre) {
    return uuid.compareTo(autre.uuid);
  }
}

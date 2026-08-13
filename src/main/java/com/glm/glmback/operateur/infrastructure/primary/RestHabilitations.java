package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.domain.PosteHabilitableId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Traduction des identifiants de postes recus par l'API.
 *
 * <p>
 * Un operateur sans habilitation est legitime : il vient d'etre declare, on lui affectera ses postes ensuite. Le champ
 * absent vaut donc l'ensemble vide.
 * </p>
 */
final class RestHabilitations {

  private RestHabilitations() {}

  static Set<PosteHabilitableId> toDomain(Set<UUID> postes) {
    return Optional.ofNullable(postes).orElseGet(Set::of).stream().map(PosteHabilitableId::new).collect(Collectors.toUnmodifiableSet());
  }
}

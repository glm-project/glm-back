package com.glm.glmback.postedetravail.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doublure de test du port vers le referentiel des natures, que ce contexte ne connait que par la donnee.
 */
final class NaturesDeclareesEnMemoire implements NaturesDeclarees {

  private final Map<NatureDeTravailId, NatureDuPoste> natures = new ConcurrentHashMap<>();

  void ajoute(NatureDuPoste nature) {
    natures.put(nature.id(), nature);
  }

  @Override
  public Optional<NatureDuPoste> get(NatureDeTravailId id) {
    return Optional.ofNullable(natures.get(id));
  }
}

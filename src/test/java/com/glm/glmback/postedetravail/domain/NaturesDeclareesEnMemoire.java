package com.glm.glmback.postedetravail.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doublure de test du port vers le referentiel des natures, que ce contexte ne connait que par la donnee.
 */
@SuppressWarnings("removal")
final class NaturesDeclareesEnMemoire implements NaturesDeclarees {

  private final Map<String, NatureDuPoste> natures = new ConcurrentHashMap<>();

  @Override
  public Optional<NatureDuPoste> get(NatureDeTravailId id) {
    return natures
      .values()
      .stream()
      .filter(nature -> nature.id().equals(id))
      .findFirst();
  }

  @Override
  public Optional<NatureDuPoste> parLibelle(NatureDeTravail libelle) {
    return Optional.ofNullable(natures.get(libelle.cle()));
  }

  @Override
  public void declare(NatureDuPoste nature) {
    natures.put(nature.libelle().cle(), nature);
  }
}

package com.glm.glmback.elementdefabrication.domain;

import java.util.UUID;

public sealed interface ElementDeFabricationId extends Comparable<ElementDeFabricationId> permits OrdreDeFabricationId, ProduitId {
  UUID uuid();

  @Override
  default int compareTo(ElementDeFabricationId other) {
    return uuid().compareTo(other.uuid());
  }
}

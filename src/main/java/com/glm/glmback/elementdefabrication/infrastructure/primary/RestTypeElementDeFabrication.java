package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ProduitId;
import java.util.UUID;

enum RestTypeElementDeFabrication {
  ORDRE_DE_FABRICATION,
  PRODUIT;

  ElementDeFabricationId toDomain(UUID uuid) {
    return switch (this) {
      case ORDRE_DE_FABRICATION -> new OrdreDeFabricationId(uuid);
      case PRODUIT -> new ProduitId(uuid);
    };
  }
}

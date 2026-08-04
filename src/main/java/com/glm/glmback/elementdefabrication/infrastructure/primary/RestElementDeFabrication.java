package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Produit;
import java.time.Instant;
import java.util.UUID;

record RestElementDeFabrication(
  RestTypeElementDeFabrication type,
  UUID id,
  String nom,
  String titre,
  String description,
  Instant dateDeCreation,
  Instant dateDeModification
) {
  static RestElementDeFabrication from(ElementDeFabrication element) {
    return new RestElementDeFabrication(
      typeDe(element),
      element.id().uuid(),
      element.nom().value(),
      element.titre().value(),
      element.description().value(),
      element.dateDeCreation(),
      element.dateDeModification()
    );
  }

  private static RestTypeElementDeFabrication typeDe(ElementDeFabrication element) {
    return switch (element) {
      case OrdreDeFabrication _ -> RestTypeElementDeFabrication.ORDRE_DE_FABRICATION;
      case Produit _ -> RestTypeElementDeFabrication.PRODUIT;
    };
  }
}

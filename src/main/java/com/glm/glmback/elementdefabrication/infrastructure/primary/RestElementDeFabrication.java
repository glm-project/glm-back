package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import java.time.Instant;
import java.util.UUID;

record RestElementDeFabrication(
  TypeDElementDeFabrication type,
  UUID id,
  String nom,
  String titre,
  String description,
  Instant dateDeCreation,
  Instant dateDeModification
) {
  static RestElementDeFabrication from(ElementDeFabrication element) {
    return new RestElementDeFabrication(
      element.type(),
      element.id().uuid(),
      element.nom().value(),
      element.titre().value(),
      element.description().value(),
      element.dateDeCreation(),
      element.dateDeModification()
    );
  }
}

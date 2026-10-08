package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Reference;
import java.time.Instant;
import java.util.UUID;

record RestElementDeFabrication(
  String categorie,
  UUID id,
  String nom,
  String reference,
  String description,
  Instant dateDeCreation,
  Instant dateDeModification
) {
  static RestElementDeFabrication from(ElementDeFabrication element) {
    return new RestElementDeFabrication(
      element.categorie().value(),
      element.id().uuid(),
      element.nom().value(),
      element.reference().map(Reference::value).orElse(null),
      element.description().map(Description::value).orElse(null),
      element.dateDeCreation(),
      element.dateDeModification()
    );
  }
}

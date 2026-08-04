package com.glm.glmback.elementdefabrication.infrastructure.primary;

import java.time.Instant;
import java.util.UUID;

record ElementDeFabricationReponse(
  String type,
  UUID id,
  String nom,
  String titre,
  String description,
  Instant dateDeCreation,
  Instant dateDeModification
) {}

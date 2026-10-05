package com.glm.glmback.atelier.application.gestionanomalies;

import java.util.Optional;
import java.util.UUID;

public interface RecusDActes {
  void create(RecuDActe recu);

  Optional<RecuDActe> get(UUID commande);
}

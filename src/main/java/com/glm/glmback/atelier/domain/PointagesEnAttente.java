package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointagesEnAttente {
  PointageEnAttente create(PointageEnAttente pointage);

  PointageEnAttente update(PointageEnAttente pointage);

  Optional<PointageEnAttente> get(PointageEnAttenteId id);

  /**
   * Les pointages mis en attente pour cet identifiant du pupitre : plusieurs quand il a ete reutilise avec des
   * contenus differents.
   */
  List<PointageEnAttente> parEvenementDuPupitre(UUID evenement);

  Page<PointageEnAttente> list(CriteresDePointageEnAttente criteres, Pageable pageable);
}

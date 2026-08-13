package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public interface OperateurRepository {
  Operateur create(Operateur operateur);

  Operateur update(Operateur operateur);

  void delete(OperateurId id);

  Optional<Operateur> get(OperateurId id);

  Optional<OperateurId> idPourIdentite(Nom nom, Prenom prenom);

  Optional<OperateurId> idPourMatricule(Matricule matricule);

  Page<Operateur> list(OperateurCriteria criteria, Pageable pageable);
}

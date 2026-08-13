package com.glm.glmback.operateur.application;

import com.glm.glmback.operateur.domain.OperateurACreer;
import com.glm.glmback.operateur.domain.OperateurAModifier;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateurRepository;
import com.glm.glmback.operateur.domain.OperateursQuiOntPointe;
import com.glm.glmback.operateur.domain.OperateursService;
import com.glm.glmback.operateur.domain.PosteHabilitableId;
import com.glm.glmback.operateur.domain.PostesHabilitables;
import com.glm.glmback.operateur.domain.ProfilDOperateur;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperateursApplicationService {

  private final OperateursService operateurs;

  public OperateursApplicationService(OperateurRepository repository, PostesHabilitables postes, OperateursQuiOntPointe pointages) {
    this.operateurs = new OperateursService(repository, postes, pointages);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ProfilDOperateur create(OperateurACreer aCreer) {
    return operateurs.create(aCreer);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public ProfilDOperateur get(OperateurId id) {
    return operateurs.get(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<ProfilDOperateur> list(Optional<PosteHabilitableId> poste, Pageable pageable) {
    return operateurs.list(poste, pageable);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ProfilDOperateur update(OperateurAModifier aModifier) {
    return operateurs.update(aModifier);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public void delete(OperateurId id) {
    operateurs.delete(id);
  }
}

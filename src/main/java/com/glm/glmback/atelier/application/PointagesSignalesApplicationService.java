package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.MotifDeSignalement;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageSignale;
import com.glm.glmback.atelier.domain.PointageSignaleId;
import com.glm.glmback.atelier.domain.PointagesSignales;
import com.glm.glmback.atelier.domain.PointagesSignalesService;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Collection;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Les pointages signales sont un ecran du gestionnaire, comme les anomalies : l'operateur n'en est pas informe, et
 * {@code ADMIN} n'a aucun acces metier.
 */
@Service
public class PointagesSignalesApplicationService {

  private final PointagesSignalesService signalements;
  private final AnnuaireDAtelierService annuaires;

  public PointagesSignalesApplicationService(
    PointagesSignales signalements,
    Clock clock,
    OperateursConnus operateurs,
    PostesConnus postes
  ) {
    this.signalements = new PointagesSignalesService(signalements, clock);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public Page<PointageSignale> list(Optional<OperateurId> operateur, Optional<MotifDeSignalement> motif, Pageable pageable) {
    return signalements.list(operateur, motif, pageable);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public PointageSignale acquitte(PointageSignaleId id, Auteur auteur) {
    return signalements.acquitte(id, auteur);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePour(Collection<PointageSignale> pointages) {
    return annuaires.pourOperateurs(pointages.stream().map(PointageSignale::operateur).toList());
  }
}

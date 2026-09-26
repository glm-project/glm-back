package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;

/**
 * La liste des pointages signales, et leur acquittement : le gestionnaire juge le pointage legitime, le temps compte
 * toujours, la ligne sort de la liste.
 */
public final class PointagesSignalesService {

  private final PointagesSignales signalements;
  private final Clock clock;

  public PointagesSignalesService(PointagesSignales signalements, Clock clock) {
    this.signalements = signalements;
    this.clock = clock;
  }

  public Page<PointageSignale> list(Optional<OperateurId> operateur, Optional<MotifDeSignalement> motif, Pageable pageable) {
    return signalements.list(new CriteresDePointageSignale(operateur, motif), pageable);
  }

  public PointageSignale acquitte(PointageSignaleId id, Auteur auteur) {
    PointageSignale signale = signalements.get(id).orElseThrow(() -> new PointageSignaleIntrouvableException(id));

    return signalements.update(signale.resolu(new Resolution(TypeDeResolution.ACQUITTE, auteur, clock.now())));
  }
}

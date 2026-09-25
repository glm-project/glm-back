package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.AnomalieDePresence;
import com.glm.glmback.atelier.domain.AnomaliesService;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SeuilDAmplitude;
import com.glm.glmback.atelier.domain.TypeDAnomalie;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Collection;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La liste des anomalies est un ecran du gestionnaire : c'est lui qui regularise. {@code ADMIN} n'a aucun acces
 * metier, et l'operateur ne voit pas les oublis des autres.
 */
@Service
public class AnomaliesApplicationService {

  private final AnomaliesService anomalies;
  private final AnnuaireDAtelierService annuaires;

  public AnomaliesApplicationService(
    JourneeDeTravailRepository journees,
    SeuilDAmplitude seuil,
    Clock clock,
    OperateursConnus operateurs,
    PostesConnus postes
  ) {
    this.anomalies = new AnomaliesService(journees, seuil, clock);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public Page<AnomalieDePresence> list(Optional<OperateurId> operateur, Optional<TypeDAnomalie> type, Pageable pageable) {
    return anomalies.list(operateur, type, pageable);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePour(Collection<AnomalieDePresence> anomalies) {
    return annuaires.pourJournees(anomalies.stream().map(AnomalieDePresence::journee).toList());
  }
}

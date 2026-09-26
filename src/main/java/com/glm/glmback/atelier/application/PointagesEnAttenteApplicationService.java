package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.GesteDAtelier;
import com.glm.glmback.atelier.domain.GesteDePresence;
import com.glm.glmback.atelier.domain.MotifDEcart;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttenteId;
import com.glm.glmback.atelier.domain.PointagesEnAttente;
import com.glm.glmback.atelier.domain.PointagesEnAttenteService;
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
 * Les pointages en attente sont un ecran du gestionnaire : il les applique par une regularisation, ou les ecarte motif
 * a l'appui. L'operateur n'en est pas informe, et {@code ADMIN} n'a aucun acces metier.
 */
@Service
public class PointagesEnAttenteApplicationService {

  private final PointagesEnAttenteService pointages;
  private final AnnuaireDAtelierService annuaires;
  private final JourneesDeTravailApplicationService presence;
  private final SuivisDAtelierApplicationService atelier;

  public PointagesEnAttenteApplicationService(
    PointagesEnAttente pointages,
    Clock clock,
    OperateursConnus operateurs,
    PostesConnus postes,
    JourneesDeTravailApplicationService presence,
    SuivisDAtelierApplicationService atelier
  ) {
    this.pointages = new PointagesEnAttenteService(pointages, clock);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
    this.presence = presence;
    this.atelier = atelier;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public Page<PointageEnAttente> list(Optional<OperateurId> operateur, Optional<MotifDeMiseEnAttente> motif, Pageable pageable) {
    return pointages.list(operateur, motif, pageable);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public PointageEnAttente applique(PointageEnAttenteId id, Auteur auteur) {
    return pointages.applique(id, auteur, pointage -> {
      switch (pointage.geste()) {
        case GesteDePresence geste -> presence.applique(geste, pointage.dateDeSurvenue(), auteur);
        case GesteDAtelier geste -> atelier.regularise(geste.regularisation(auteur, pointage.dateDeSurvenue()));
      }
    });
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public PointageEnAttente ecarte(PointageEnAttenteId id, Auteur auteur, MotifDEcart motif) {
    return pointages.ecarte(id, auteur, motif);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePour(Collection<PointageEnAttente> enAttente) {
    return annuaires.pourOperateurs(
      enAttente
        .stream()
        .map(pointage -> pointage.geste().operateur())
        .toList()
    );
  }
}

package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.ClotureAEnregistrer;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EngagementAEnregistrer;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.IssueDePointage;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageDAtelierTraite;
import com.glm.glmback.atelier.domain.PointageIgnoreException;
import com.glm.glmback.atelier.domain.PointagesIgnores;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.RegularisationTraitee;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.SuivisDAtelierService;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDurations;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orchestration des actes portant sur un element engage en atelier.
 *
 * <p>
 * Le pointage est ouvert a l'operateur ; l'engagement, la cloture et la regularisation sont reserves au
 * gestionnaire. C'est la frontiere entre les deux surfaces de l'API.
 * </p>
 *
 * <p>
 * C'est ici que se lit l'horloge des lectures : chaque suivi rendu l'est a l'instant d'evaluation du moment, que le
 * domaine recoit explicitement.
 * </p>
 */
@Service
public class SuivisDAtelierApplicationService {

  private final SuivisDAtelierService suivisDAtelier;
  private final AnnuaireDAtelierService annuaires;
  private final TransactionTemplate transactions;
  private final Clock clock;

  public SuivisDAtelierApplicationService(
    SuiviDAtelierRepository repository,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    PointagesIgnores pointagesIgnores,
    MaximumActivityDurations dureesMax,
    Clock clock,
    TransactionTemplate transactions
  ) {
    this.suivisDAtelier = SuivisDAtelierService.builder()
      .repository(repository)
      .elements(elements)
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .pointagesIgnores(pointagesIgnores)
      .dureeMax(dureesMax)
      .clock(clock);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
    this.transactions = transactions;
    this.clock = clock;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public LectureDuSuivi engage(EngagementAEnregistrer commande) {
    return lu(suivisDAtelier.engage(commande));
  }

  /**
   * Le pointage se juge dans sa propre transaction, qui ecrit l'audit d'un pointage ignore. Le refus n'est leve qu'une
   * fois cette transaction validee : une exception levee dedans l'aurait annulee, audit compris.
   */
  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  public ResultatDEcriture<LectureDuSuivi> pointeDuPupitre(PointageAEnregistrer commande) {
    PointageDAtelierTraite traite = SaisieConcurrenteRejouee.executer(transactions, () -> suivisDAtelier.pointe(commande));
    if (traite.issue() == IssueDePointage.IGNORE) {
      throw new PointageIgnoreException(commande.evenement());
    }

    return new ResultatDEcriture<>(lu(traite.suivi()), traite.issue() == IssueDePointage.REJOUE);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ResultatDEcriture<LectureDuSuivi> regularise(RegularisationAEnregistrer commande) {
    RegularisationTraitee traitee = suivisDAtelier.regularise(commande);

    return new ResultatDEcriture<>(lu(traitee.suivi()), traitee.rejeu());
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public LectureDuSuivi cloture(ClotureAEnregistrer commande) {
    return lu(suivisDAtelier.cloture(commande));
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public LectureDuSuivi annuleLaCloture(SuiviDAtelierId id) {
    return lu(suivisDAtelier.annuleLaCloture(id));
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public LectureDuSuivi get(SuiviDAtelierId id) {
    return lu(suivisDAtelier.get(id));
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<LectureDuSuivi> list(Optional<Periode> periode, Set<EtatDAtelier> etats, Pageable pageable) {
    Instant evaluation = clock.now();
    Page<SuiviDAtelier> suivis = suivisDAtelier.list(periode, etats, evaluation, pageable);

    return Page.<LectureDuSuivi>builder()
      .content(
        suivis
          .content()
          .stream()
          .map(suivi -> new LectureDuSuivi(suivi, evaluation))
          .toList()
      )
      .currentPage(suivis.currentPage())
      .pageSize(suivis.pageSize())
      .totalElementsCount(suivis.totalElementsCount());
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePour(SuiviDAtelier suivi) {
    return annuaires.pour(suivi);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePourSuivis(Collection<SuiviDAtelier> suivis) {
    return annuaires.pourSuivis(suivis);
  }

  private LectureDuSuivi lu(SuiviDAtelier suivi) {
    return new LectureDuSuivi(suivi, clock.now());
  }
}

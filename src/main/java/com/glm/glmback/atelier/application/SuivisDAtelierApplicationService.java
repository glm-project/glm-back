package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.ClotureAEnregistrer;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EngagementAEnregistrer;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.FonctionsDesOperateurs;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.SuivisDAtelierService;
import com.glm.glmback.atelier.domain.TempsDAtelierService;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration des actes portant sur un element engage en atelier.
 *
 * <p>
 * Le pointage est ouvert a l'operateur ; l'engagement, la cloture et les trois actes de correction sont reserves au
 * gestionnaire. C'est la frontiere entre les deux surfaces de l'API.
 * </p>
 */
@Service
public class SuivisDAtelierApplicationService {

  private final SuivisDAtelierService suivisDAtelier;
  private final TempsDAtelierService tempsDAtelier;

  public SuivisDAtelierApplicationService(
    SuiviDAtelierRepository repository,
    JourneeDeTravailRepository journees,
    ElementsEngageables elements,
    FonctionsDesOperateurs fonctions,
    Clock clock
  ) {
    this.suivisDAtelier = SuivisDAtelierService.builder().repository(repository).elements(elements).fonctions(fonctions).clock(clock);
    this.tempsDAtelier = new TempsDAtelierService(repository, journees);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier engage(EngagementAEnregistrer commande) {
    return suivisDAtelier.engage(commande);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public SuiviDAtelier pointe(PointageAEnregistrer commande) {
    return suivisDAtelier.pointe(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier regularise(RegularisationAEnregistrer commande) {
    return suivisDAtelier.regularise(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier annule(AnnulationAEnregistrer commande) {
    return suivisDAtelier.annule(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier corrige(CorrectionAEnregistrer commande) {
    return suivisDAtelier.corrige(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier cloture(ClotureAEnregistrer commande) {
    return suivisDAtelier.cloture(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier annuleLaCloture(SuiviDAtelierId id) {
    return suivisDAtelier.annuleLaCloture(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public SuiviDAtelier get(SuiviDAtelierId id) {
    return suivisDAtelier.get(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<SuiviDAtelier> list(Optional<Periode> periode, Set<EtatDAtelier> etats, Pageable pageable) {
    return suivisDAtelier.list(periode, etats, pageable);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public List<IntervalleDActivite> tempsEffectif(SuiviDAtelierId id) {
    return tempsDAtelier.tempsEffectif(id);
  }
}

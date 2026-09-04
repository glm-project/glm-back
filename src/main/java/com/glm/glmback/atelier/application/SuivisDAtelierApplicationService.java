package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.ClotureAEnregistrer;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EngagementAEnregistrer;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.SuivisDAtelierService;
import com.glm.glmback.atelier.domain.TempsDAtelierService;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
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
  private final AnnuaireDAtelierService annuaires;
  private final IdentitesDEvenements identites;

  public SuivisDAtelierApplicationService(
    SuiviDAtelierRepository repository,
    JourneeDeTravailRepository journees,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    Clock clock,
    IdentitesDEvenements identites
  ) {
    this.suivisDAtelier = SuivisDAtelierService.builder()
      .repository(repository)
      .elements(elements)
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .clock(clock);
    this.tempsDAtelier = new TempsDAtelierService(repository, journees);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
    this.identites = identites;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier engage(EngagementAEnregistrer commande) {
    return suivisDAtelier.engage(commande);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public SuiviDAtelier pointe(PointageAEnregistrer commande) {
    return pointeDuPupitre(commande).agregat();
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public ResultatDEcriture<SuiviDAtelier> pointeDuPupitre(PointageAEnregistrer commande) {
    ReservationDEvenement reservation = identites.reserve(
      commande.evenement().uuid(),
      new EmpreinteDEvenement(
        NatureDeGesteDuPupitre.POINTAGE_D_ATELIER,
        Optional.of(commande.suivi().uuid()),
        commande.operateur().uuid(),
        commande.type().name(),
        commande.poste().map(poste -> poste.uuid()),
        commande.dateDeSurvenue()
      )
    );
    if (reservation.estUnRejeu()) {
      return new ResultatDEcriture<>(suivisDAtelier.get(new SuiviDAtelierId(reservation.agregat().orElseThrow().id())), true);
    }
    SuiviDAtelier suivi = suivisDAtelier.pointe(commande);
    identites.associe(commande.evenement().uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suivi.id().uuid()));
    return new ResultatDEcriture<>(suivi, false);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier regularise(RegularisationAEnregistrer commande) {
    UUID evenement = reserveIdentiteServeur();
    SuiviDAtelier suivi = suivisDAtelier.regularise(commande, new com.glm.glmback.atelier.domain.EvenementDAtelierId(evenement));
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suivi.id().uuid()));
    return suivi;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier annule(AnnulationAEnregistrer commande) {
    return suivisDAtelier.annule(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier corrige(CorrectionAEnregistrer commande) {
    UUID evenement = reserveIdentiteServeur();
    SuiviDAtelier suivi = suivisDAtelier.corrige(commande, new com.glm.glmback.atelier.domain.EvenementDAtelierId(evenement));
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suivi.id().uuid()));
    return suivi;
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

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePourIntervalles(Collection<IntervalleDActivite> intervalles) {
    return annuaires.pourIntervalles(intervalles);
  }

  private UUID reserveIdentiteServeur() {
    return Stream.generate(UUID::randomUUID).filter(identites::reserveHorsPupitre).findFirst().orElseThrow();
  }
}

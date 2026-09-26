package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.ClotureAEnregistrer;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EngagementAEnregistrer;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.GesteDAtelier;
import com.glm.glmback.atelier.domain.GesteRecu;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageDAtelierTraite;
import com.glm.glmback.atelier.domain.PointagesEnAttente;
import com.glm.glmback.atelier.domain.PointagesEnAttenteService;
import com.glm.glmback.atelier.domain.PointagesSignales;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SeuilDAmplitude;
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
import org.springframework.transaction.support.TransactionTemplate;

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
  private final GestesDuPupitre gestes;
  private final IdentitesDEvenements identites;
  private final TransactionTemplate transactions;

  public SuivisDAtelierApplicationService(
    SuiviDAtelierRepository repository,
    JourneeDeTravailRepository journees,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    SeuilDAmplitude seuil,
    PointagesSignales signalements,
    PointagesEnAttente enAttente,
    Clock clock,
    IdentitesDEvenements identites,
    TransactionTemplate transactions
  ) {
    this.suivisDAtelier = SuivisDAtelierService.builder()
      .repository(repository)
      .elements(elements)
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .signalements(signalements)
      .clock(clock);
    this.tempsDAtelier = TempsDAtelierService.builder().suivis(repository).journees(journees).seuil(seuil).clock(clock);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
    this.gestes = new GestesDuPupitre(new PointagesEnAttenteService(enAttente, clock), identites);
    this.identites = identites;
    this.transactions = transactions;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public SuiviDAtelier engage(EngagementAEnregistrer commande) {
    return suivisDAtelier.engage(commande);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  public ResultatDEcriture<SuiviDAtelier> pointeDuPupitre(PointageAEnregistrer commande) {
    GesteRecu recu = new GesteRecu(
      commande.evenement().uuid(),
      GesteDAtelier.builder()
        .suivi(commande.suivi())
        .operateur(commande.operateur())
        .type(commande.type())
        .poste(commande.poste())
        .dateDeclaree(commande.dateDeSurvenue()),
      commande.auteur()
    );
    EmpreinteDEvenement empreinte = EmpreinteDEvenement.builder()
      .nature(NatureDeGesteDuPupitre.POINTAGE_D_ATELIER)
      .cible(Optional.of(commande.suivi().uuid()))
      .operateur(commande.operateur().uuid())
      .type(commande.type().name())
      .poste(commande.poste().map(poste -> poste.uuid()))
      .dateDeSurvenue(commande.dateDeSurvenue());

    return SaisieConcurrenteRejouee.executer(transactions, () ->
      gestes.ecrit(
        recu,
        empreinte,
        id -> suivisDAtelier.get(new SuiviDAtelierId(id)),
        () -> {
          PointageDAtelierTraite traite = suivisDAtelier.pointe(commande);
          identites.associe(
            commande.evenement().uuid(),
            new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, traite.suivi().id().uuid())
          );
          return new ResultatDEcriture<>(traite.suivi(), traite.absorbe());
        }
      )
    );
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

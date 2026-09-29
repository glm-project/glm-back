package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.ActiviteId;
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
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageDAtelierTraite;
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
import java.time.Instant;
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
 *
 * <p>
 * C'est ici que se lit l'horloge des lectures : chaque suivi rendu l'est a l'instant d'evaluation du moment, que le
 * domaine recoit explicitement.
 * </p>
 */
@Service
public class SuivisDAtelierApplicationService {

  private final SuivisDAtelierService suivisDAtelier;
  private final TempsDAtelierService tempsDAtelier;
  private final AnnuaireDAtelierService annuaires;
  private final IdentitesDEvenements identites;
  private final TransactionTemplate transactions;
  private final Clock clock;

  public SuivisDAtelierApplicationService(
    SuiviDAtelierRepository repository,
    JourneeDeTravailRepository journees,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    SeuilDAmplitude seuil,
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
      .clock(clock);
    this.tempsDAtelier = TempsDAtelierService.builder().suivis(repository).journees(journees).seuil(seuil);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
    this.identites = identites;
    this.transactions = transactions;
    this.clock = clock;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public LectureDuSuivi engage(EngagementAEnregistrer commande) {
    return lu(suivisDAtelier.engage(commande));
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public LectureDuSuivi pointe(PointageAEnregistrer commande) {
    return pointeDuPupitre(commande).agregat();
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  public ResultatDEcriture<LectureDuSuivi> pointeDuPupitre(PointageAEnregistrer commande) {
    return SaisieConcurrenteRejouee.executer(transactions, () -> {
      ReservationDEvenement reservation = identites.reserve(
        commande.evenement().uuid(),
        EmpreinteDEvenement.builder()
          .nature(NatureDeGesteDuPupitre.POINTAGE_D_ATELIER)
          .suivi(Optional.of(commande.suivi().uuid()))
          .operateur(commande.operateur().uuid())
          .type(commande.type().name())
          .intention(Optional.of(commande.intention().name()))
          .activiteVisee(commande.activiteVisee().map(ActiviteId::uuid))
          .poste(commande.poste().map(poste -> poste.uuid()))
          .dateDeSurvenue(commande.dateDeSurvenue())
      );
      if (reservation.estUnRejeu()) {
        return new ResultatDEcriture<>(lu(suivisDAtelier.get(new SuiviDAtelierId(reservation.agregat().orElseThrow().id()))), true);
      }
      PointageDAtelierTraite traite = suivisDAtelier.pointe(commande);
      identites.associe(
        commande.evenement().uuid(),
        new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, traite.suivi().id().uuid())
      );
      return new ResultatDEcriture<>(lu(traite.suivi()), traite.absorbe());
    });
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public LectureDuSuivi regularise(RegularisationAEnregistrer commande) {
    UUID evenement = reserveIdentiteServeur();
    SuiviDAtelier suivi = suivisDAtelier.regularise(commande, new com.glm.glmback.atelier.domain.EvenementDAtelierId(evenement));
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suivi.id().uuid()));
    return lu(suivi);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public LectureDuSuivi annule(AnnulationAEnregistrer commande) {
    return lu(suivisDAtelier.annule(commande));
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public LectureDuSuivi corrige(CorrectionAEnregistrer commande) {
    UUID evenement = reserveIdentiteServeur();
    SuiviDAtelier suivi = suivisDAtelier.corrige(commande, new com.glm.glmback.atelier.domain.EvenementDAtelierId(evenement));
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suivi.id().uuid()));
    return lu(suivi);
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
  @Transactional(readOnly = true)
  public LectureDuSuivi get(SuiviDAtelierId id) {
    return lu(suivisDAtelier.get(id));
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<LectureDuSuivi> list(Optional<Periode> periode, Set<EtatDAtelier> etats, Pageable pageable) {
    Instant evaluation = clock.now();
    Page<SuiviDAtelier> suivis = suivisDAtelier.list(periode, etats, pageable);

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
  public List<IntervalleDActivite> tempsEffectif(SuiviDAtelierId id) {
    return tempsDAtelier.tempsEffectif(id, clock.now());
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

  private LectureDuSuivi lu(SuiviDAtelier suivi) {
    return new LectureDuSuivi(suivi, clock.now());
  }

  private UUID reserveIdentiteServeur() {
    return Stream.generate(UUID::randomUUID).filter(identites::reserveHorsPupitre).findFirst().orElseThrow();
  }
}

package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.AnnulationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.CorrectionDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.JourneesDeTravailService;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.RegularisationDePresenceAEnregistrer;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la presence des operateurs.
 *
 * <p>
 * Arrivee, pause, reprise et depart sont des gestes de l'operateur lui-meme ; leur correction appartient au
 * gestionnaire.
 * </p>
 */
@Service
public class JourneesDeTravailApplicationService {

  private final JourneesDeTravailService journeesDeTravail;
  private final AnnuaireDAtelierService annuaires;
  private final IdentitesDEvenements identites;

  public JourneesDeTravailApplicationService(
    JourneeDeTravailRepository repository,
    OperateursConnus operateurs,
    PostesConnus postes,
    Clock clock,
    IdentitesDEvenements identites
  ) {
    this.journeesDeTravail = new JourneesDeTravailService(repository, operateurs, clock);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
    this.identites = identites;
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public JourneeDeTravail arrive(ArriveeAEnregistrer commande) {
    return arriveDuPupitre(commande).agregat();
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public ResultatDEcriture<JourneeDeTravail> arriveDuPupitre(ArriveeAEnregistrer commande) {
    ReservationDEvenement reservation = identites.reserve(
      commande.evenement().uuid(),
      new EmpreinteDEvenement(
        NatureDeGesteDuPupitre.ARRIVEE,
        Optional.empty(),
        commande.operateur().uuid(),
        commande.type().name(),
        Optional.empty(),
        commande.dateDeSurvenue()
      )
    );
    if (reservation.estUnRejeu()) {
      return new ResultatDEcriture<>(journeesDeTravail.get(new JourneeDeTravailId(reservation.agregat().orElseThrow().id())), true);
    }
    JourneeDeTravail journee = journeesDeTravail.arrive(commande);
    identites.associe(commande.evenement().uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee.id().uuid()));
    return new ResultatDEcriture<>(journee, false);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public JourneeDeTravail pointe(PointageDePresenceAEnregistrer commande) {
    return pointeDuPupitre(commande).agregat();
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional
  public ResultatDEcriture<JourneeDeTravail> pointeDuPupitre(PointageDePresenceAEnregistrer commande) {
    ReservationDEvenement reservation = identites.reserve(
      commande.evenement().uuid(),
      new EmpreinteDEvenement(
        NatureDeGesteDuPupitre.POINTAGE_DE_PRESENCE,
        Optional.empty(),
        commande.operateur().uuid(),
        commande.type().name(),
        Optional.empty(),
        commande.dateDeSurvenue()
      )
    );
    if (reservation.estUnRejeu()) {
      return new ResultatDEcriture<>(journeesDeTravail.get(new JourneeDeTravailId(reservation.agregat().orElseThrow().id())), true);
    }
    JourneeDeTravail journee = journeesDeTravail.pointe(commande);
    identites.associe(commande.evenement().uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee.id().uuid()));
    return new ResultatDEcriture<>(journee, false);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande) {
    UUID evenement = reserveIdentiteServeur();
    JourneeDeTravail journee = journeesDeTravail.regularise(commande, new com.glm.glmback.atelier.domain.EvenementDePresenceId(evenement));
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee.id().uuid()));
    return journee;
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail annule(AnnulationDePresenceAEnregistrer commande) {
    return journeesDeTravail.annule(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail corrige(CorrectionDePresenceAEnregistrer commande) {
    UUID evenement = reserveIdentiteServeur();
    JourneeDeTravail journee = journeesDeTravail.corrige(commande, new com.glm.glmback.atelier.domain.EvenementDePresenceId(evenement));
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee.id().uuid()));
    return journee;
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public JourneeDeTravail get(JourneeDeTravailId id) {
    return journeesDeTravail.get(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<JourneeDeTravail> list(Optional<Periode> periode, Optional<OperateurId> operateur, Pageable pageable) {
    return journeesDeTravail.list(periode, operateur, pageable);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePour(JourneeDeTravail journee) {
    return annuaires.pour(journee);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public AnnuaireDAtelier annuairePourJournees(Collection<JourneeDeTravail> journees) {
    return annuaires.pourJournees(journees);
  }

  private UUID reserveIdentiteServeur() {
    return Stream.generate(UUID::randomUUID).filter(identites::reserveHorsPupitre).findFirst().orElseThrow();
  }
}

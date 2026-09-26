package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnnuaireDAtelierService;
import com.glm.glmback.atelier.domain.AnnulationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CorrectionDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.GesteDePresence;
import com.glm.glmback.atelier.domain.GesteRecu;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.JourneesDeTravailService;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.PointagesEnAttente;
import com.glm.glmback.atelier.domain.PointagesEnAttenteService;
import com.glm.glmback.atelier.domain.PointagesSignales;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.PresenceTraitee;
import com.glm.glmback.atelier.domain.RegularisationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.SeuilDAmplitude;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
  private final GestesDuPupitre gestes;
  private final IdentitesDEvenements identites;
  private final TransactionTemplate transactions;

  public JourneesDeTravailApplicationService(
    JourneeDeTravailRepository repository,
    OperateursConnus operateurs,
    PostesConnus postes,
    SeuilDAmplitude seuil,
    PointagesSignales signalements,
    PointagesEnAttente enAttente,
    Clock clock,
    IdentitesDEvenements identites,
    TransactionTemplate transactions
  ) {
    this.journeesDeTravail = JourneesDeTravailService.builder()
      .repository(repository)
      .operateurs(operateurs)
      .seuil(seuil)
      .signalements(signalements)
      .clock(clock);
    this.annuaires = new AnnuaireDAtelierService(operateurs, postes);
    this.gestes = new GestesDuPupitre(new PointagesEnAttenteService(enAttente, clock), identites);
    this.identites = identites;
    this.transactions = transactions;
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  public ResultatDEcriture<JourneeDeTravail> arriveDuPupitre(ArriveeAEnregistrer commande) {
    GesteRecu recu = new GesteRecu(
      commande.evenement().uuid(),
      new GesteDePresence(commande.operateur(), TypeDEvenementDePresence.ARRIVEE, commande.dateDeSurvenue()),
      commande.auteur()
    );
    EmpreinteDEvenement empreinte = empreinte(
      NatureDeGesteDuPupitre.ARRIVEE,
      commande.operateur(),
      commande.type().name(),
      commande.dateDeSurvenue()
    );

    return SaisieConcurrenteRejouee.executer(transactions, () ->
      gestes.ecrit(recu, empreinte, this::journee, () -> {
        PresenceTraitee arrivee = journeesDeTravail.arrive(commande);
        associe(commande.evenement().uuid(), arrivee.journee());
        return new ResultatDEcriture<>(arrivee.journee(), arrivee.absorbee());
      })
    );
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  public ResultatDEcriture<JourneeDeTravail> pointeDuPupitre(PointageDePresenceAEnregistrer commande) {
    GesteRecu recu = new GesteRecu(
      commande.evenement().uuid(),
      new GesteDePresence(commande.operateur(), commande.type(), commande.dateDeSurvenue()),
      commande.auteur()
    );
    EmpreinteDEvenement empreinte = empreinte(
      NatureDeGesteDuPupitre.POINTAGE_DE_PRESENCE,
      commande.operateur(),
      commande.type().name(),
      commande.dateDeSurvenue()
    );

    return SaisieConcurrenteRejouee.executer(transactions, () ->
      gestes.ecrit(recu, empreinte, this::journee, () -> {
        PresenceTraitee traitee = journeesDeTravail.pointe(commande, () -> new EvenementDePresenceId(reserveIdentiteServeur()));
        associe(commande.evenement().uuid(), traitee.journee());
        return new ResultatDEcriture<>(traitee.journee(), traitee.absorbee());
      })
    );
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande) {
    UUID evenement = reserveIdentiteServeur();
    JourneeDeTravail journee = journeesDeTravail.regularise(commande, new EvenementDePresenceId(evenement));
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee.id().uuid()));
    return journee;
  }

  /**
   * Un geste de presence mis en attente, applique par le gestionnaire : une regularisation sous une identite serveur.
   */
  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public JourneeDeTravail applique(GesteDePresence geste, Instant dateDeSurvenue, Auteur auteur) {
    UUID evenement = reserveIdentiteServeur();
    JourneeDeTravail journee = journeesDeTravail.applique(geste, dateDeSurvenue, auteur, new EvenementDePresenceId(evenement));
    associe(evenement, journee);
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
    JourneeDeTravail journee = journeesDeTravail.corrige(commande, new EvenementDePresenceId(evenement));
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

  private JourneeDeTravail journee(UUID id) {
    return journeesDeTravail.get(new JourneeDeTravailId(id));
  }

  private void associe(UUID evenement, JourneeDeTravail journee) {
    identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee.id().uuid()));
  }

  private static EmpreinteDEvenement empreinte(NatureDeGesteDuPupitre nature, OperateurId operateur, String type, Optional<Instant> date) {
    return EmpreinteDEvenement.builder()
      .nature(nature)
      .cible(Optional.empty())
      .operateur(operateur.uuid())
      .type(type)
      .poste(Optional.empty())
      .dateDeSurvenue(date);
  }

  private UUID reserveIdentiteServeur() {
    return Stream.generate(UUID::randomUUID).filter(identites::reserveHorsPupitre).findFirst().orElseThrow();
  }
}

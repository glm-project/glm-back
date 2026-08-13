package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * La fabrique de domaine de la presence : c'est ici, et nulle part ailleurs, que se decide une date de survenue.
 *
 * <p>
 * Un pointage se date sur l'horloge, une regularisation sur la valeur fournie ; la date d'enregistrement vaut
 * l'instant present dans les deux cas. L'agregat, lui, ne voit qu'un evenement deja horodate.
 * </p>
 *
 * <p>
 * La presence ne connait aucun poste de travail, donc aucune habilitation : seule l'existence de l'operateur est
 * verifiee, et une seule fois, a l'ouverture de la journee.
 * </p>
 */
public final class JourneesDeTravailService {

  private final JourneeDeTravailRepository repository;
  private final OperateursConnus operateurs;
  private final Clock clock;

  public JourneesDeTravailService(JourneeDeTravailRepository repository, OperateursConnus operateurs, Clock clock) {
    this.repository = repository;
    this.operateurs = operateurs;
    this.clock = clock;
  }

  public JourneeDeTravail arrive(ArriveeAEnregistrer commande) {
    if (!operateurs.existe(commande.operateur())) {
      throw new OperateurDAtelierIntrouvableException(commande.operateur());
    }

    if (repository.getEnCoursPour(commande.operateur()).isPresent()) {
      throw new JourneeDeTravailDejaOuverteException(commande.operateur());
    }

    Instant maintenant = clock.now();
    Horodatage horodatage = new Horodatage(commande.dateDeSurvenue().orElse(maintenant), maintenant);

    return repository.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), commande.operateur()).enregistre(
        evenement(TypeDEvenementDePresence.ARRIVEE, commande.auteur(), horodatage)
      )
    );
  }

  public JourneeDeTravail pointe(PointageDePresenceAEnregistrer commande) {
    JourneeDeTravail journee = repository
      .getEnCoursPour(commande.operateur())
      .orElseThrow(() -> new AucuneJourneeDeTravailEnCoursException(commande.operateur()));

    return repository.update(journee.enregistre(evenement(commande.type(), commande.auteur(), Horodatage.saisiA(clock.now()))));
  }

  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande) {
    return repository.update(get(commande.journee()).enregistre(regularisation(commande)));
  }

  public JourneeDeTravail annule(AnnulationDePresenceAEnregistrer commande) {
    return repository.update(get(commande.journee()).annule(commande.evenement(), annulation(commande.auteur(), commande.motif())));
  }

  public JourneeDeTravail corrige(CorrectionDePresenceAEnregistrer commande) {
    RegularisationDePresenceAEnregistrer remplacement = commande.remplacement();

    return repository.update(
      get(remplacement.journee()).corrige(
        commande.evenement(),
        annulation(remplacement.auteur(), commande.motif()),
        regularisation(remplacement)
      )
    );
  }

  public JourneeDeTravail get(JourneeDeTravailId id) {
    return repository.get(id).orElseThrow(() -> new JourneeDeTravailIntrouvableException(id));
  }

  public Page<JourneeDeTravail> list(Optional<Periode> periode, Optional<OperateurId> operateur, Pageable pageable) {
    return repository.list(new JourneeDeTravailCriteria(periode, operateur), pageable);
  }

  private EvenementDePresence regularisation(RegularisationDePresenceAEnregistrer commande) {
    return evenement(commande.type(), commande.auteur(), new Horodatage(commande.dateDeSurvenue(), clock.now()));
  }

  private Annulation annulation(Auteur auteur, MotifDAnnulation motif) {
    return new Annulation(auteur, clock.now(), motif);
  }

  private static EvenementDePresence evenement(TypeDEvenementDePresence type, Auteur auteur, Horodatage horodatage) {
    return EvenementDePresence.builder().id(EvenementDePresenceId.newId()).type(type).auteur(auteur).horodatage(horodatage);
  }
}

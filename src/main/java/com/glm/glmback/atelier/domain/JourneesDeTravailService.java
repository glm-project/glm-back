package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

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
  private final SeuilDAmplitude seuil;
  private final Clock clock;

  private JourneesDeTravailService(JourneeDeTravailRepository repository, OperateursConnus operateurs, SeuilDAmplitude seuil, Clock clock) {
    this.repository = repository;
    this.operateurs = operateurs;
    this.seuil = seuil;
    this.clock = clock;
  }

  public static JourneesDeTravailServiceRepositoryBuilder builder() {
    return repository -> operateurs -> seuil -> clock -> new JourneesDeTravailService(repository, operateurs, seuil, clock);
  }

  /**
   * Une arrivee sous le seuil est redondante et absorbee : l'operateur est deja la. Au-dela, la journee en cours est
   * abandonnee et l'arrivee en ouvre une nouvelle. C'est ce qui permet a un poste de nuit de se reidentifier a 3 h,
   * et a l'arrivee du lendemain d'un depart oublie de n'etre plus jamais perdue.
   */
  public ArriveeTraitee arrive(ArriveeAEnregistrer commande) {
    if (!operateurs.existe(commande.operateur())) {
      throw new OperateurDAtelierIntrouvableException(commande.operateur());
    }

    Horodatage horodatage = horodatage(commande.dateDeSurvenue());
    Optional<JourneeDeTravail> enCours = repository
      .getEnCoursPour(commande.operateur())
      .filter(journee -> !journee.estAbandonneePour(horodatage.dateDeSurvenue(), seuil.amplitudeMaximale()));

    if (enCours.isPresent()) {
      return new ArriveeTraitee(enCours.orElseThrow(), true);
    }

    JourneeDeTravail ouverte = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), commande.operateur()).enregistre(
      evenement(commande.evenement(), TypeDEvenementDePresence.ARRIVEE, commande.auteur(), horodatage)
    );

    return new ArriveeTraitee(repository.create(ouverte), false);
  }

  public JourneeDeTravail pointe(PointageDePresenceAEnregistrer commande) {
    return pointe(commande, EvenementDePresenceId::newId);
  }

  /**
   * Un geste recu pour une journee abandonnee n'est jamais refuse : il ouvre une nouvelle journee par une arrivee
   * implicite a l'heure du geste, puis s'y applique. Une reprise, qui suppose une pause, s'y reduit a l'arrivee.
   *
   * <p>
   * L'identifiant de l'arrivee implicite n'est demande que lorsque la decision est prise : il appartient au serveur,
   * jamais au pupitre.
   * </p>
   */
  public JourneeDeTravail pointe(PointageDePresenceAEnregistrer commande, Supplier<EvenementDePresenceId> arriveeImplicite) {
    JourneeDeTravail journee = repository
      .getEnCoursPour(commande.operateur())
      .orElseThrow(() -> new AucuneJourneeDeTravailEnCoursException(commande.operateur()));

    Horodatage horodatage = horodatage(commande.dateDeSurvenue());
    EvenementDePresence geste = evenement(commande.evenement(), commande.type(), commande.auteur(), horodatage);

    if (journee.estAbandonneePour(horodatage.dateDeSurvenue(), seuil.amplitudeMaximale())) {
      return repository.create(
        nouvelleJournee(commande, geste, evenement(arriveeImplicite.get(), TypeDEvenementDePresence.ARRIVEE, commande.auteur(), horodatage))
      );
    }

    return repository.update(journee.enregistre(geste));
  }

  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande) {
    return regularise(commande, EvenementDePresenceId.newId());
  }

  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande, EvenementDePresenceId evenement) {
    return repository.update(sansChevauchement(get(commande.journee()).enregistre(regularisation(commande, evenement))));
  }

  public JourneeDeTravail annule(AnnulationDePresenceAEnregistrer commande) {
    return repository.update(get(commande.journee()).annule(commande.evenement(), annulation(commande.auteur(), commande.motif())));
  }

  public JourneeDeTravail corrige(CorrectionDePresenceAEnregistrer commande) {
    return corrige(commande, EvenementDePresenceId.newId());
  }

  public JourneeDeTravail corrige(CorrectionDePresenceAEnregistrer commande, EvenementDePresenceId remplacementId) {
    RegularisationDePresenceAEnregistrer remplacement = commande.remplacement();

    return repository.update(
      sansChevauchement(
        get(remplacement.journee()).corrige(
          commande.evenement(),
          annulation(remplacement.auteur(), commande.motif()),
          regularisation(remplacement, remplacementId)
        )
      )
    );
  }

  public JourneeDeTravail get(JourneeDeTravailId id) {
    return repository.get(id).orElseThrow(() -> new JourneeDeTravailIntrouvableException(id));
  }

  public Page<JourneeDeTravail> list(Optional<Periode> periode, Optional<OperateurId> operateur, Pageable pageable) {
    return repository.list(new JourneeDeTravailCriteria(periode, operateur), pageable);
  }

  private static JourneeDeTravail nouvelleJournee(
    PointageDePresenceAEnregistrer commande,
    EvenementDePresence geste,
    EvenementDePresence arrivee
  ) {
    JourneeDeTravail ouverte = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), commande.operateur()).enregistre(arrivee);

    return geste.type() == TypeDEvenementDePresence.REPRISE ? ouverte : ouverte.enregistre(geste);
  }

  /**
   * Un acte du gestionnaire ne fait jamais chevaucher deux journees d'un meme operateur : l'etendue modifiee, du
   * premier au dernier fait connu, ne touche celle d'aucune autre.
   */
  private JourneeDeTravail sansChevauchement(JourneeDeTravail journee) {
    journee
      .etendue()
      .flatMap(etendue ->
        repository
          .journeesDeLOperateurSur(journee.operateur(), etendue)
          .stream()
          .filter(autre -> !autre.id().equals(journee.id()))
          .findFirst()
      )
      .ifPresent(autre -> {
        throw new ChevauchementDeJourneesException(journee, autre);
      });

    return journee;
  }

  private Horodatage horodatage(Optional<Instant> dateDeSurvenue) {
    Instant maintenant = clock.now();
    refuseDateFuture(dateDeSurvenue, maintenant);

    return new Horodatage(dateDeSurvenue.orElse(maintenant), maintenant);
  }

  private EvenementDePresence regularisation(RegularisationDePresenceAEnregistrer commande, EvenementDePresenceId evenement) {
    return evenement(evenement, commande.type(), commande.auteur(), new Horodatage(commande.dateDeSurvenue(), clock.now()));
  }

  private Annulation annulation(Auteur auteur, MotifDAnnulation motif) {
    return new Annulation(auteur, clock.now(), motif);
  }

  private static void refuseDateFuture(Optional<Instant> dateDeSurvenue, Instant maintenant) {
    if (dateDeSurvenue.filter(date -> date.isAfter(maintenant)).isPresent()) {
      throw new DateDeSurvenueFutureException(dateDeSurvenue.orElseThrow());
    }
  }

  private static EvenementDePresence evenement(
    EvenementDePresenceId id,
    TypeDEvenementDePresence type,
    Auteur auteur,
    Horodatage horodatage
  ) {
    return EvenementDePresence.builder().id(id).type(type).auteur(auteur).horodatage(horodatage);
  }

  public interface JourneesDeTravailServiceRepositoryBuilder {
    JourneesDeTravailServiceOperateursBuilder repository(JourneeDeTravailRepository repository);
  }

  public interface JourneesDeTravailServiceOperateursBuilder {
    JourneesDeTravailServiceSeuilBuilder operateurs(OperateursConnus operateurs);
  }

  public interface JourneesDeTravailServiceSeuilBuilder {
    JourneesDeTravailServiceClockBuilder seuil(SeuilDAmplitude seuil);
  }

  public interface JourneesDeTravailServiceClockBuilder {
    JourneesDeTravailService clock(Clock clock);
  }
}

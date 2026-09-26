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
  private final RegistreDesSignalements signalements;
  private final Clock clock;

  private JourneesDeTravailService(
    JourneeDeTravailRepository repository,
    OperateursConnus operateurs,
    SeuilDAmplitude seuil,
    PointagesSignales signalements,
    Clock clock
  ) {
    this.repository = repository;
    this.operateurs = operateurs;
    this.seuil = seuil;
    this.signalements = new RegistreDesSignalements(signalements, clock);
    this.clock = clock;
  }

  public static JourneesDeTravailServiceRepositoryBuilder builder() {
    return repository ->
      operateurs -> seuil -> signalements -> clock -> new JourneesDeTravailService(repository, operateurs, seuil, signalements, clock);
  }

  /**
   * Une arrivee sous le seuil est redondante et absorbee : l'operateur est deja la. Au-dela, la journee en cours est
   * abandonnee et l'arrivee en ouvre une nouvelle. C'est ce qui permet a un poste de nuit de se reidentifier a 3 h,
   * et a l'arrivee du lendemain d'un depart oublie de n'etre plus jamais perdue.
   */
  public PresenceTraitee arrive(ArriveeAEnregistrer commande) {
    if (!operateurs.existe(commande.operateur())) {
      throw new OperateurDAtelierIntrouvableException(commande.operateur());
    }

    RegistreDesSignalements.DateRedressee date = redresse(commande.dateDeSurvenue());
    Optional<JourneeDeTravail> enCours = repository
      .getEnCoursPour(commande.operateur())
      .filter(journee -> !journee.estAbandonneePour(date.horodatage().dateDeSurvenue(), seuil.amplitudeMaximale()));

    if (enCours.isPresent()) {
      return new PresenceTraitee(enCours.orElseThrow(), true);
    }

    JourneeDeTravail ouverte = repository.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), commande.operateur()).enregistre(
        evenement(commande.evenement(), TypeDEvenementDePresence.ARRIVEE, commande.auteur(), date.horodatage())
      )
    );
    signale(commande.evenement(), ouverte, date);

    return new PresenceTraitee(ouverte, false);
  }

  public PresenceTraitee pointe(PointageDePresenceAEnregistrer commande) {
    return pointe(commande, EvenementDePresenceId::newId);
  }

  /**
   * Un geste de presence n'est jamais refuse a l'operateur (lots 3 et 8a de la strategie « bornes de fin de
   * journee »).
   *
   * <ul>
   * <li>Sans journee en cours, ou sur une journee abandonnee, il ouvre une nouvelle journee par une arrivee implicite
   * a l'heure du geste, puis s'y applique. Une reprise s'y reduit a l'arrivee.</li>
   * <li>Redondant avec l'etat courant — une pause deja en pause, une reprise deja present —, il est absorbe : rien
   * n'est ajoute au journal.</li>
   * </ul>
   *
   * <p>
   * Restent refuses ici les gestes qu'on ne sait pas rattacher : un operateur inconnu, et un geste rejoue dans le
   * desordre, date avant le dernier fait connu ou dans une journee deja fermee. {@link PointagesEnAttenteService} les
   * met en attente (lot 8c).
   * </p>
   *
   * <p>
   * L'identifiant de l'arrivee implicite n'est demande que lorsque la decision est prise : il appartient au serveur,
   * jamais au pupitre.
   * </p>
   */
  public PresenceTraitee pointe(PointageDePresenceAEnregistrer commande, Supplier<EvenementDePresenceId> arriveeImplicite) {
    RegistreDesSignalements.DateRedressee date = redresse(commande.dateDeSurvenue());
    Horodatage horodatage = date.horodatage();
    Instant geste = horodatage.dateDeSurvenue();
    EvenementDePresence evenement = evenement(commande.evenement(), commande.type(), commande.auteur(), horodatage);
    Optional<JourneeDeTravail> enCours = repository.getEnCoursPour(commande.operateur());

    if (enCours.isEmpty()) {
      exigeUnGesteRattachable(commande.operateur(), geste);
    }

    if (enCours.filter(journee -> !journee.estAbandonneePour(geste, seuil.amplitudeMaximale())).isEmpty()) {
      EvenementDePresence arrivee = evenement(arriveeImplicite.get(), TypeDEvenementDePresence.ARRIVEE, commande.auteur(), horodatage);
      JourneeDeTravail ouverte = repository.create(nouvelleJournee(commande, evenement, arrivee));
      signale(commande.evenement(), ouverte, date);

      return new PresenceTraitee(ouverte, false);
    }

    JourneeDeTravail journee = enCours.orElseThrow();
    if (estRedondant(journee, evenement)) {
      return new PresenceTraitee(journee, true);
    }

    JourneeDeTravail pointee = repository.update(journee.enregistre(evenement));
    signale(commande.evenement(), pointee, date);

    return new PresenceTraitee(pointee, false);
  }

  private void signale(EvenementDePresenceId evenement, JourneeDeTravail journee, RegistreDesSignalements.DateRedressee date) {
    signalements.signale(
      evenement.uuid(),
      new CibleDuSignalement(TypeDeCible.JOURNEE_DE_TRAVAIL, journee.id().uuid()),
      journee.operateur(),
      date
    );
  }

  private void exigeUnGesteRattachable(OperateurId operateur, Instant geste) {
    if (!operateurs.existe(operateur)) {
      throw new OperateurDAtelierIntrouvableException(operateur);
    }

    if (!repository.journeesDeLOperateurSur(operateur, new Periode(geste, geste)).isEmpty()) {
      throw new AucuneJourneeDeTravailEnCoursException(operateur);
    }
  }

  /**
   * Redondant : l'etat courant n'admet pas ce geste, et il n'est pas date avant le dernier fait connu. Date avant, il
   * serait un geste rejoue dans le desordre, que l'automate refuse.
   */
  private static boolean estRedondant(JourneeDeTravail journee, EvenementDePresence evenement) {
    return (
      journee.etat().apres(evenement.type()).isEmpty()
      && journee
        .etendue()
        .filter(etendue -> evenement.dateDeSurvenue().isBefore(etendue.fin()))
        .isEmpty()
    );
  }

  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande) {
    return regularise(commande, EvenementDePresenceId.newId());
  }

  public JourneeDeTravail regularise(RegularisationDePresenceAEnregistrer commande, EvenementDePresenceId evenement) {
    return repository.update(sansChevauchement(get(commande.journee()).enregistre(regularisation(commande, evenement))));
  }

  /**
   * Applique un geste de presence mis en attente, au nom du gestionnaire : c'est une regularisation, refusable avec
   * explication comme toute autre. Une arrivee ouvre sa journee ; tout autre geste s'inscrit dans la journee qui
   * contient sa date, a defaut dans la journee en cours.
   */
  public JourneeDeTravail applique(GesteDePresence geste, Instant dateDeSurvenue, Auteur auteur, EvenementDePresenceId evenement) {
    if (geste.type() == TypeDEvenementDePresence.ARRIVEE) {
      return arrive(new ArriveeAEnregistrer(geste.operateur(), auteur, Optional.of(dateDeSurvenue), evenement)).journee();
    }
    if (!operateurs.existe(geste.operateur())) {
      throw new OperateurDAtelierIntrouvableException(geste.operateur());
    }

    JourneeDeTravail journee = repository
      .journeesDeLOperateurSur(geste.operateur(), new Periode(dateDeSurvenue, dateDeSurvenue))
      .stream()
      .findFirst()
      .or(() -> repository.getEnCoursPour(geste.operateur()))
      .orElseThrow(() -> new AucuneJourneeDeTravailEnCoursException(geste.operateur()));

    return regularise(
      RegularisationDePresenceAEnregistrer.builder().journee(journee.id()).type(geste.type()).auteur(auteur).dateDeSurvenue(dateDeSurvenue),
      evenement
    );
  }

  public JourneeDeTravail annule(AnnulationDePresenceAEnregistrer commande) {
    JourneeDeTravail annulee = repository.update(
      get(commande.journee()).annule(commande.evenement(), annulation(commande.auteur(), commande.motif()))
    );
    signalements.resout(commande.evenement().uuid(), TypeDeResolution.ANNULE, commande.auteur());

    return annulee;
  }

  public JourneeDeTravail corrige(CorrectionDePresenceAEnregistrer commande) {
    return corrige(commande, EvenementDePresenceId.newId());
  }

  public JourneeDeTravail corrige(CorrectionDePresenceAEnregistrer commande, EvenementDePresenceId remplacementId) {
    RegularisationDePresenceAEnregistrer remplacement = commande.remplacement();

    JourneeDeTravail corrigee = repository.update(
      sansChevauchement(
        get(remplacement.journee()).corrige(
          commande.evenement(),
          annulation(remplacement.auteur(), commande.motif()),
          regularisation(remplacement, remplacementId)
        )
      )
    );
    signalements.resout(commande.evenement().uuid(), TypeDeResolution.CORRIGE, remplacement.auteur());

    return corrigee;
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

    return seReduitALArrivee(geste) ? ouverte : ouverte.enregistre(geste);
  }

  /**
   * Une reprise suppose une pause, et une arrivee egaree sur la route des pointages double l'arrivee implicite : sur
   * une nouvelle journee, l'une comme l'autre se reduisent a cette arrivee.
   */
  private static boolean seReduitALArrivee(EvenementDePresence geste) {
    return geste.type() == TypeDEvenementDePresence.REPRISE || geste.type() == TypeDEvenementDePresence.ARRIVEE;
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

  /**
   * Un geste du pupitre date dans le futur, horloge en avance, n'est plus refuse : il est ramene a sa reception et
   * signale au gestionnaire.
   */
  private RegistreDesSignalements.DateRedressee redresse(Optional<Instant> dateDeSurvenue) {
    return RegistreDesSignalements.redresse(dateDeSurvenue, clock.now(), Optional.empty());
  }

  private EvenementDePresence regularisation(RegularisationDePresenceAEnregistrer commande, EvenementDePresenceId evenement) {
    return evenement(evenement, commande.type(), commande.auteur(), new Horodatage(commande.dateDeSurvenue(), clock.now()));
  }

  private Annulation annulation(Auteur auteur, MotifDAnnulation motif) {
    return new Annulation(auteur, clock.now(), motif);
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
    JourneesDeTravailServiceSignalementsBuilder seuil(SeuilDAmplitude seuil);
  }

  public interface JourneesDeTravailServiceSignalementsBuilder {
    JourneesDeTravailServiceClockBuilder signalements(PointagesSignales signalements);
  }

  public interface JourneesDeTravailServiceClockBuilder {
    JourneesDeTravailService clock(Clock clock);
  }
}

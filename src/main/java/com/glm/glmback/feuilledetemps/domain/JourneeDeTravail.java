package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Une venue de l'operateur, et les fenetres de presence qu'on en deduit.
 *
 * <p>
 * Une journee de travail n'est pas un jour du calendrier : elle va d'une arrivee a un depart, et peut passer minuit.
 * C'est le decoupage calendaire, plus tard, qui la ramene aux jours de la semaine ; ici, aucune date, seulement des
 * instants — exactement comme dans l'atelier d'ou ces evenements viennent.
 * </p>
 *
 * <p>
 * Le journal reste la source de verite : les colonnes {@code debut} et {@code fin} de la table ne servent qu'a borner
 * la requete, jamais a reconstruire la presence.
 * </p>
 */
public record JourneeDeTravail(List<EvenementDePresence> journal, Optional<Instant> finPresumee) {
  /**
   * A instant egal, l'arrivee passe devant : l'arrivee implicite d'un geste tardif partage l'heure de ce geste, et la
   * base les rend sans les departager.
   */
  private static final Comparator<EvenementDePresence> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(
    EvenementDePresence::dateDeSurvenue
  ).thenComparing(EvenementDePresence::type);

  private static final Duration JOURNEE_INVRAISEMBLABLE = Duration.ofHours(24);

  public JourneeDeTravail {
    Assert.field("journal", journal).notNull().noNullElement();
    Assert.notNull("fin presumee", finPresumee);
    journal = journal.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
    fenetres(journal);
  }

  public JourneeDeTravail(List<EvenementDePresence> journal) {
    this(journal, Optional.empty());
  }

  /**
   * Vrai si la journee, toujours sans depart, a depasse le seuil a cet instant : son amplitude depuis l'arrivee est
   * strictement superieure au seuil.
   */
  public boolean estAbandonneePour(Instant instant, AmplitudeMaximale seuil) {
    return !estFermee() && fenetreDeRecherche(seuil).flatMap(Plage::fin).filter(instant::isAfter).isPresent();
  }

  /**
   * Vrai si la journee se lit a sa fin presumee : abandonnee a cet instant, ou fermee plus de 24 h apres son arrivee.
   * Une telle journee n'a pas pu etre vecue d'une traite, et son depart ne dit rien de l'heure a laquelle l'operateur
   * est vraiment parti (issue #59).
   */
  public boolean estPresumeePour(Instant instant, AmplitudeMaximale seuil) {
    return estAbandonneePour(instant, seuil) || estInvraisemblable();
  }

  /**
   * De l'arrivee a l'arrivee plus le seuil : la ou se cherche le dernier fait connu d'une journee abandonnee.
   */
  public Optional<Plage> fenetreDeRecherche(AmplitudeMaximale seuil) {
    return premierFait().map(arrivee -> new Plage(arrivee, Optional.of(arrivee.plus(seuil.value()))));
  }

  /**
   * La meme journee, fermee a sa fin presumee : son dernier fait de presence, ou le dernier pointage d'OF s'il est
   * plus tardif et tombe dans la fenetre de recherche. Sa derniere fenetre ouverte, s'il y en a une, devient presumee.
   */
  public JourneeDeTravail presumee(AmplitudeMaximale seuil, Optional<Instant> dernierPointage) {
    Optional<Plage> recherche = fenetreDeRecherche(seuil);
    if (recherche.isEmpty()) {
      return this;
    }

    Instant dernierFait = dernierFaitRetenu(recherche.orElseThrow());
    Instant fin = dernierPointage
      .filter(recherche.orElseThrow()::contient)
      .filter(pointage -> pointage.isAfter(dernierFait))
      .orElse(dernierFait);

    return new JourneeDeTravail(journal, Optional.of(fin));
  }

  /**
   * Vrai si cet instant tombe entre l'arrivee et le depart pointes, une journee sans depart n'ayant pas de borne
   * haute. Comme dans l'atelier, la fin presumee ne borne pas la journee : elle ne coupe que ses fenetres.
   */
  public boolean contient(Instant instant) {
    return (
      premierFait()
        .filter(arrivee -> !instant.isBefore(arrivee))
        .isPresent()
      && dernier()
        .filter(evenement -> evenement.type() == TypeDEvenementDePresence.DEPART)
        .filter(depart -> instant.isAfter(depart.dateDeSurvenue()))
        .isEmpty()
    );
  }

  public Optional<Instant> arrivee() {
    return premierFait();
  }

  /**
   * Les intervalles ou l'operateur etait present, de chaque arrivee a son depart, dans l'ordre.
   */
  public List<Plage> fenetres() {
    return presumees(fenetres(journal));
  }

  /**
   * Les fenetres coupees a la fin presumee : celle qui la franchit, ou reste ouverte, s'y arrete et devient presumee,
   * celles qui commencent apres disparaissent.
   */
  private List<Plage> presumees(List<Plage> fenetres) {
    return finPresumee
      .map(fin ->
        fenetres
          .stream()
          .filter(fenetre -> !fenetre.debut().isAfter(fin))
          .map(fenetre -> coupee(fenetre, fin))
          .toList()
      )
      .orElse(fenetres);
  }

  private static Plage coupee(Plage fenetre, Instant fin) {
    if (
      fenetre
        .fin()
        .filter(date -> !date.isAfter(fin))
        .isPresent()
    ) {
      return fenetre;
    }

    return new Plage(fenetre.debut(), Optional.of(fin), true);
  }

  /**
   * Fermee plus de 24 h apres son arrivee : une borne physique, et non un parametre de l'entreprise, que l'amplitude
   * maximale ne peut jamais atteindre.
   */
  private boolean estInvraisemblable() {
    return estFermee() && dernierFait().orElseThrow().isAfter(premierFait().orElseThrow().plus(JOURNEE_INVRAISEMBLABLE));
  }

  /**
   * Le dernier fait connu d'une journee ouverte, quel qu'il soit : un fait regularise au-dela du seuil reste un fait.
   * Celui d'une journee fermee se cherche dans la fenetre de recherche, puisque c'est son depart qu'on ne croit pas.
   */
  private Instant dernierFaitRetenu(Plage recherche) {
    if (!estFermee()) {
      return dernierFait().orElseThrow();
    }

    return faits()
      .map(EvenementDePresence::dateDeSurvenue)
      .filter(recherche::contient)
      .reduce((precedent, suivant) -> suivant)
      .orElseThrow();
  }

  private boolean estFermee() {
    return dernier()
      .filter(evenement -> evenement.type() == TypeDEvenementDePresence.DEPART)
      .isPresent();
  }

  private Optional<Instant> premierFait() {
    return journal.stream().findFirst().map(EvenementDePresence::dateDeSurvenue);
  }

  private Stream<EvenementDePresence> faits() {
    return journal.stream();
  }

  private Optional<Instant> dernierFait() {
    return dernier().map(EvenementDePresence::dateDeSurvenue);
  }

  private Optional<EvenementDePresence> dernier() {
    return journal.stream().reduce((precedent, suivant) -> suivant);
  }

  private static List<Plage> fenetres(List<EvenementDePresence> evenements) {
    List<Plage> fenetres = new ArrayList<>();
    EtatDePresence etat = EtatDePresence.ABSENT;

    for (int rang = 0; rang < evenements.size(); rang++) {
      EvenementDePresence evenement = evenements.get(rang);
      EtatDePresence avant = etat;
      etat = avant.apres(evenement.type()).orElseThrow(() -> new TransitionDePresenceInterditeException(evenement, avant));

      if (etat == EtatDePresence.PRESENT) {
        fenetres.add(new Plage(evenement.dateDeSurvenue(), suivant(evenements, rang)));
      }
    }

    return List.copyOf(fenetres);
  }

  /**
   * La fin d'une fenetre est l'evenement suivant, le depart qui la referme. Sans suivant, l'operateur n'est pas encore
   * parti et la fenetre reste ouverte.
   */
  private static Optional<Instant> suivant(List<EvenementDePresence> evenements, int rang) {
    if (rang + 1 == evenements.size()) {
      return Optional.empty();
    }

    return Optional.of(evenements.get(rang + 1).dateDeSurvenue());
  }
}

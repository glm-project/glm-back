package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
   * Vrai si la journee, toujours sans depart, a depasse le seuil a cet instant : son amplitude depuis l'arrivee,
   * pauses comprises, est strictement superieure au seuil.
   */
  public boolean estAbandonneePour(Instant instant, AmplitudeMaximale seuil) {
    return !estFermee() && fenetreDeRecherche(seuil).flatMap(Plage::fin).filter(instant::isAfter).isPresent();
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

    Instant dernierFait = dernierFait().orElseThrow();
    Instant fin = dernierPointage
      .filter(recherche.orElseThrow()::contient)
      .filter(pointage -> pointage.isAfter(dernierFait))
      .orElse(dernierFait);

    return new JourneeDeTravail(journal, Optional.of(fin));
  }

  /**
   * Les intervalles ou l'operateur etait present et non en pause, dans l'ordre.
   */
  public List<Plage> fenetres() {
    return presumees(fenetres(journal));
  }

  private List<Plage> presumees(List<Plage> fenetres) {
    return fenetres
      .stream()
      .map(fenetre ->
        finPresumee
          .filter(fin -> fenetre.estOuverte())
          .map(fin -> new Plage(fenetre.debut(), Optional.of(fin), true))
          .orElse(fenetre)
      )
      .toList();
  }

  private boolean estFermee() {
    return dernier()
      .filter(evenement -> evenement.type() == TypeDEvenementDePresence.DEPART)
      .isPresent();
  }

  private Optional<Instant> premierFait() {
    return journal.stream().findFirst().map(EvenementDePresence::dateDeSurvenue);
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
   * La fin d'une fenetre est l'evenement suivant, quel qu'il soit : une pause comme un depart la referment. Sans
   * suivant, l'operateur n'est pas encore parti et la fenetre reste ouverte.
   */
  private static Optional<Instant> suivant(List<EvenementDePresence> evenements, int rang) {
    if (rang + 1 == evenements.size()) {
      return Optional.empty();
    }

    return Optional.of(evenements.get(rang + 1).dateDeSurvenue());
  }
}

package com.glm.glmback.syntheseheures.domain;

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
 * <strong>Le repli est tolerant, a la difference de son jumeau {@code feuilledetemps}</strong> : un pointage qui ne
 * s'enchaine pas selon l'automate de presence ne fait jamais echouer la lecture — le releve doit rester genere quoi
 * qu'il arrive. Ce pointage est simplement ignore, silencieusement, comme s'il n'existait pas : ni retenu dans
 * {@link #pointages()}, ni compte dans {@link #fenetres()}. En pratique ce cas ne se produit jamais via l'API —
 * {@code atelier} valide tout le journal a chaque ecriture — donc ce repli tolerant ne joue qu'en defense en
 * profondeur, sur une donnee qui existerait hors du chemin applicatif normal.
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
  }

  public JourneeDeTravail(List<EvenementDePresence> journal) {
    this(journal, Optional.empty());
  }

  /**
   * Les pointages valides du journal, dans l'ordre chronologique. Un pointage dont l'enchainement casse l'automate
   * de presence n'y figure pas.
   */
  public List<EvenementDePresence> pointages() {
    return repli().pointages();
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
    return presumees(repli().fenetres());
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
    return pointages().stream().findFirst().map(EvenementDePresence::dateDeSurvenue);
  }

  private Optional<Instant> dernierFait() {
    return dernier().map(EvenementDePresence::dateDeSurvenue);
  }

  private Optional<EvenementDePresence> dernier() {
    return pointages()
      .stream()
      .reduce((precedent, suivant) -> suivant);
  }

  private Repli repli() {
    List<EvenementDePresence> pointages = new ArrayList<>();
    List<Plage> fenetres = new ArrayList<>();
    EtatDePresence etat = EtatDePresence.ABSENT;
    Instant debutFenetre = null;

    for (EvenementDePresence evenement : journal) {
      Optional<EtatDePresence> apres = etat.apres(evenement.type());

      if (apres.isEmpty()) {
        continue; // pointage fautif : ignore silencieusement, n'entre dans aucun des deux resultats
      }

      pointages.add(evenement);

      EtatDePresence nouvelEtat = apres.get();
      // L'automate ne mappe jamais PRESENT sur PRESENT (voir EtatDePresence) : verifier l'etat d'arrivee suffit,
      // sans re-verifier l'etat de depart en plus — les deux conditions ne peuvent jamais etre vraies ensemble.
      if (nouvelEtat == EtatDePresence.PRESENT) {
        debutFenetre = evenement.dateDeSurvenue();
      } else if (etat == EtatDePresence.PRESENT) {
        fenetres.add(new Plage(debutFenetre, Optional.of(evenement.dateDeSurvenue())));
      }
      etat = nouvelEtat;
    }

    if (etat == EtatDePresence.PRESENT) {
      fenetres.add(new Plage(debutFenetre, Optional.empty()));
    }

    return new Repli(List.copyOf(pointages), List.copyOf(fenetres));
  }

  private record Repli(List<EvenementDePresence> pointages, List<Plage> fenetres) {}
}

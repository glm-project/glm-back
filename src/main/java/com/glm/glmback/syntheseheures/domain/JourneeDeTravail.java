package com.glm.glmback.syntheseheures.domain;

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

  private static final Duration JOURNEE_INVRAISEMBLABLE = Duration.ofHours(24);

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
   * Les intervalles ou l'operateur etait present et non en pause, dans l'ordre.
   */
  public List<Plage> fenetres() {
    return presumees(repli().fenetres());
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
    return pointages().stream().findFirst().map(EvenementDePresence::dateDeSurvenue);
  }

  private Stream<EvenementDePresence> faits() {
    return pointages().stream();
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

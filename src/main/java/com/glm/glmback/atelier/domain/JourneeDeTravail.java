package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * La presence d'un operateur dans l'entreprise, de son arrivee a son depart.
 *
 * <p>
 * Ses bornes sont l'arrivee et le depart, jamais le jour calendaire : aucun fuseau horaire n'entre ainsi dans le
 * domaine, et un operateur qui repasse le soir ouvre simplement une seconde journee. C'est ce que le client decrit,
 * les heures de presence courant de l'arrivee dans la societe au depart, et non du premier au dernier element.
 * </p>
 */
public record JourneeDeTravail(JourneeDeTravailId id, OperateurId operateur, JournalDePresence journal) {
  private static final Duration JOURNEE_INVRAISEMBLABLE = Duration.ofHours(24);

  public JourneeDeTravail {
    Assert.notNull("id", id);
    Assert.notNull("operateur", operateur);
    Assert.notNull("journal", journal);
  }

  public static JourneeDeTravail ouverte(JourneeDeTravailId id, OperateurId operateur) {
    return new JourneeDeTravail(id, operateur, JournalDePresence.vide());
  }

  public JourneeDeTravail enregistre(EvenementDePresence evenement) {
    return new JourneeDeTravail(id, operateur, journal.enregistre(evenement));
  }

  public JourneeDeTravail annule(EvenementDePresenceId evenement, Annulation annulation) {
    return new JourneeDeTravail(id, operateur, journal.annule(evenement, annulation));
  }

  public JourneeDeTravail corrige(EvenementDePresenceId evenement, Annulation annulation, EvenementDePresence remplacant) {
    return new JourneeDeTravail(id, operateur, journal.corrige(evenement, annulation, remplacant));
  }

  public List<FenetreDePresence> fenetres() {
    return journal.fenetres();
  }

  public Optional<Periode> amplitude() {
    return journal.amplitude();
  }

  public Optional<Instant> debut() {
    return journal.debut();
  }

  public EtatDePresence etat() {
    return journal.etat();
  }

  /**
   * Vrai si la journee, toujours sans depart, a depasse le seuil a cet instant : son amplitude depuis l'arrivee est
   * strictement superieure au seuil. Un geste recu a ce moment ouvre une nouvelle journee.
   */
  public boolean estAbandonneePour(Instant instant, AmplitudeMaximale seuil) {
    return (
      estEnCours()
      && debut()
        .filter(arrivee -> instant.isAfter(arrivee.plus(seuil.value())))
        .isPresent()
    );
  }

  /**
   * Vrai si la journee se lit a sa fin presumee : abandonnee a cet instant, ou fermee plus de 24 h apres son arrivee.
   * Une telle journee n'a pas pu etre vecue d'une traite, et son depart ne dit rien de l'heure a laquelle l'operateur
   * est vraiment parti (issue #59). Elle n'est pas abandonnee pour autant : un geste recu ensuite ne la concerne pas,
   * et l'anomalie reste une amplitude excessive.
   */
  public boolean estPresumeePour(Instant instant, AmplitudeMaximale seuil) {
    return estAbandonneePour(instant, seuil) || estInvraisemblable();
  }

  /**
   * Fermee plus de 24 h apres son arrivee : une borne physique, et non un parametre de l'entreprise, que l'amplitude
   * maximale ne peut jamais atteindre.
   */
  private boolean estInvraisemblable() {
    return amplitude()
      .filter(bornes -> bornes.fin().isAfter(bornes.debut().plus(JOURNEE_INVRAISEMBLABLE)))
      .isPresent();
  }

  /**
   * Du premier au dernier fait connu : c'est sur elle que se juge le chevauchement de deux journees. Une journee
   * abandonnee sans depart s'arrete a son dernier geste, elle ne deborde pas sur la suivante.
   */
  public Optional<Periode> etendue() {
    return journal.etendue();
  }

  /**
   * De l'arrivee a l'arrivee plus le seuil : la ou se cherche le dernier fait connu d'une journee abandonnee.
   */
  public Optional<Periode> fenetreDeRecherche(AmplitudeMaximale seuil) {
    return debut().map(arrivee -> new Periode(arrivee, arrivee.plus(seuil.value())));
  }

  /**
   * Les fenetres de presence lues a cet instant. Une journee abandonnee se ferme a sa fin presumee, le dernier fait
   * connu : son dernier evenement, ou le dernier pointage d'OF de l'operateur s'il est plus tardif et reste dans la
   * fenetre de recherche. La fenetre ainsi fermee est presumee. Une journee encore sous le seuil reste ouverte : c'est
   * du travail en cours.
   */
  public List<FenetreDePresence> fenetresA(Instant maintenant, AmplitudeMaximale seuil, Optional<Instant> dernierPointage) {
    if (!estPresumeePour(maintenant, seuil)) {
      return fenetres();
    }

    Instant finPresumee = finPresumee(seuil, dernierPointage);

    return fenetres()
      .stream()
      .filter(fenetre -> !fenetre.debut().isAfter(finPresumee))
      .map(fenetre -> coupee(fenetre, finPresumee))
      .toList();
  }

  /**
   * Celle qui franchit la fin presumee, ou reste ouverte, s'y arrete et devient presumee ; une fenetre deja close avant
   * reste pointee.
   */
  private static FenetreDePresence coupee(FenetreDePresence fenetre, Instant finPresumee) {
    if (
      fenetre
        .fin()
        .filter(fin -> !fin.isAfter(finPresumee))
        .isPresent()
    ) {
      return fenetre;
    }

    return new FenetreDePresence(fenetre.debut(), Optional.of(finPresumee), true);
  }

  private Instant finPresumee(AmplitudeMaximale seuil, Optional<Instant> dernierPointage) {
    Periode recherche = fenetreDeRecherche(seuil).orElseThrow();
    Instant dernierFait = dernierFaitRetenu(recherche);

    return dernierPointage
      .filter(recherche::contains)
      .filter(pointage -> pointage.isAfter(dernierFait))
      .orElse(dernierFait);
  }

  /**
   * Le dernier fait connu d'une journee en cours, quel qu'il soit : un fait regularise au-dela du seuil reste un fait.
   * Celui d'une journee fermee se cherche dans la fenetre de recherche, puisque c'est son depart qu'on ne croit pas.
   */
  private Instant dernierFaitRetenu(Periode recherche) {
    if (estEnCours()) {
      return etendue().orElseThrow().fin();
    }

    return journal.dernierFaitDans(recherche).orElseThrow();
  }

  public boolean estEnCours() {
    return etat() != EtatDePresence.ABSENT;
  }

  /**
   * Vrai si cet instant tombe entre l'arrivee et le depart, une journee encore ouverte n'ayant pas de borne haute.
   */
  public boolean contient(Instant instant) {
    return commenceAvant(instant) && !seTerminaAvant(instant);
  }

  private boolean commenceAvant(Instant instant) {
    return debut()
      .filter(arrivee -> !instant.isBefore(arrivee))
      .isPresent();
  }

  private boolean seTerminaAvant(Instant instant) {
    return amplitude()
      .filter(bornes -> instant.isAfter(bornes.fin()))
      .isPresent();
  }
}

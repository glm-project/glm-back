package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
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
   * Vrai si la journee, toujours sans depart, a depasse le seuil a cet instant : son amplitude depuis l'arrivee,
   * pauses comprises, est strictement superieure au seuil. Un geste recu a ce moment ouvre une nouvelle journee.
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
   * fenetre de recherche. La fenetre ainsi fermee est presumee ; une journee restee en pause n'en a aucune a fermer.
   * Une journee encore sous le seuil reste ouverte : c'est du travail en cours.
   */
  public List<FenetreDePresence> fenetresA(Instant maintenant, AmplitudeMaximale seuil, Optional<Instant> dernierPointage) {
    if (!estAbandonneePour(maintenant, seuil)) {
      return fenetres();
    }

    Instant finPresumee = finPresumee(seuil, dernierPointage);

    return fenetres()
      .stream()
      .map(fenetre -> fenetre.estOuverte() ? new FenetreDePresence(fenetre.debut(), Optional.of(finPresumee), true) : fenetre)
      .toList();
  }

  private Instant finPresumee(AmplitudeMaximale seuil, Optional<Instant> dernierPointage) {
    Instant dernierFait = etendue().orElseThrow().fin();
    Periode recherche = fenetreDeRecherche(seuil).orElseThrow();

    return dernierPointage
      .filter(recherche::contains)
      .filter(pointage -> pointage.isAfter(dernierFait))
      .orElse(dernierFait);
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

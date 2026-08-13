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

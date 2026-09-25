package com.glm.glmback.coutderevient.domain;

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
 * Ce contexte ne la ramene a aucune date — il ne mesure que des durees, et une duree n'a pas besoin de savoir a quel
 * jour elle appartient.
 * </p>
 *
 * <p>
 * Le journal reste la source de verite : les colonnes {@code debut} et {@code fin} de la table ne servent qu'a borner
 * la requete, jamais a reconstruire la presence.
 * </p>
 */
public record JourneeDeTravail(List<EvenementDePresence> journal, Optional<Instant> finPresumee) {
  private static final Comparator<EvenementDePresence> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDePresence::dateDeSurvenue);

  public JourneeDeTravail {
    Assert.field("journal", journal).notNull().noNullElement();
    Assert.notNull("fin presumee", finPresumee);
    journal = journal.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
    fenetres(journal);
    List<EvenementDePresence> evenements = journal;
    finPresumee.ifPresent(fin -> {
      Assert.notEmpty("journal d'une journee presumee", evenements);
      Assert.field("fin presumee", fin).afterOrAt(evenements.getLast().dateDeSurvenue());
    });
  }

  /**
   * Une venue telle que la base la relit : aucune fin presumee, c'est la lecture qui en decide.
   */
  public JourneeDeTravail(List<EvenementDePresence> journal) {
    this(journal, Optional.empty());
  }

  /**
   * La meme venue lue a cet instant. Sans depart et au-dela du seuil, elle est abandonnee et recoit une fin presumee :
   * son dernier evenement, ou le dernier pointage d'OF de l'operateur s'il est plus tardif et tombe entre l'arrivee et
   * l'arrivee plus le seuil. La nuit n'est alors plus valorisee.
   */
  public JourneeDeTravail presumee(Instant maintenant, AmplitudeMaximale seuil, List<Instant> pointagesDeLOperateur) {
    if (journal.isEmpty() || estFermee()) {
      return this;
    }

    Instant arrivee = journal.getFirst().dateDeSurvenue();
    Instant limite = arrivee.plus(seuil.value());
    if (!maintenant.isAfter(limite)) {
      return this;
    }

    Instant dernierFait = journal.getLast().dateDeSurvenue();
    Instant fin = pointagesDeLOperateur
      .stream()
      .filter(pointage -> !pointage.isBefore(arrivee) && !pointage.isAfter(limite))
      .filter(pointage -> pointage.isAfter(dernierFait))
      .max(Comparator.naturalOrder())
      .orElse(dernierFait);

    return new JourneeDeTravail(journal, Optional.of(fin));
  }

  /**
   * Les intervalles ou l'operateur etait present et non en pause, dans l'ordre.
   */
  public List<Plage> fenetres() {
    return fenetres(journal)
      .stream()
      .map(fenetre ->
        finPresumee
          .filter(fin -> fenetre.fin().isEmpty())
          .map(fin -> new Plage(fenetre.debut(), Optional.of(fin)))
          .orElse(fenetre)
      )
      .toList();
  }

  /**
   * Vrai si cet instant tombe entre l'arrivee et le depart, une journee encore ouverte n'ayant pas de borne haute.
   *
   * <p>
   * C'est ce qui rattache un travail a la venue pendant laquelle il a commence, et donc ce qui empeche un pointage
   * jamais arrete de courir jusqu'au lendemain : l'operateur reclique sur l'element a son retour.
   * </p>
   */
  public boolean contient(Instant instant) {
    return commenceAvant(instant) && !seTerminaAvant(instant);
  }

  private boolean commenceAvant(Instant instant) {
    return journal
      .stream()
      .findFirst()
      .filter(arrivee -> !instant.isBefore(arrivee.dateDeSurvenue()))
      .isPresent();
  }

  /**
   * La journee n'a de borne haute que si son dernier evenement l'a refermee, ou si elle a recu une fin presumee : sans
   * l'un ni l'autre, l'operateur n'est pas encore parti, et rien ne dit qu'il ne travaillait plus.
   */
  private boolean seTerminaAvant(Instant instant) {
    return fin().filter(instant::isAfter).isPresent();
  }

  /**
   * Le depart, ou a defaut la fin presumee d'une venue abandonnee.
   */
  private Optional<Instant> fin() {
    return depart().or(() -> finPresumee);
  }

  private boolean estFermee() {
    return depart().isPresent();
  }

  private Optional<Instant> depart() {
    return journal
      .stream()
      .reduce((precedent, dernier) -> dernier)
      .filter(evenement -> evenement.type() == TypeDEvenementDePresence.DEPART)
      .map(EvenementDePresence::dateDeSurvenue);
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

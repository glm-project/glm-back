package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
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
 *
 * <p>
 * Le calcul du cout ne doit jamais echouer sur la presence : un geste que l'automate refuse — un depart sans arrivee,
 * une seconde arrivee — est ecarte du journal a la construction, et la journee se lit sur ce qui reste. C'est a
 * l'atelier de refuser ou de corriger, jamais a la lecture.
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
    journal = retenus(journal.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList());
    List<EvenementDePresence> evenements = journal;
    finPresumee.ifPresent(fin -> {
      Assert.notEmpty("journal d'une journee presumee", evenements);
      Assert.field("fin presumee", fin).afterOrAt(evenements.getFirst().dateDeSurvenue());
    });
  }

  /**
   * Une venue telle que la base la relit : aucune fin presumee, c'est la lecture qui en decide.
   */
  public JourneeDeTravail(List<EvenementDePresence> journal) {
    this(journal, Optional.empty());
  }

  /**
   * La meme venue lue a cet instant. Sans depart et au-dela du seuil, elle est abandonnee ; fermee plus de 24 h apres
   * son arrivee, elle n'a pas pu etre vecue d'une traite (issue #59). L'une comme l'autre recoit une fin presumee : son
   * dernier fait connu, ou le dernier pointage d'OF de l'operateur s'il est plus tardif et tombe entre l'arrivee et
   * l'arrivee plus le seuil. La nuit n'est alors plus valorisee.
   */
  public JourneeDeTravail presumee(Instant maintenant, AmplitudeMaximale seuil, List<Instant> pointagesDeLOperateur) {
    if (journal.isEmpty()) {
      return this;
    }

    Instant arrivee = journal.getFirst().dateDeSurvenue();
    Instant limite = arrivee.plus(seuil.value());
    if (!estPresumeeA(maintenant, limite)) {
      return this;
    }

    Instant dernierFait = dernierFaitRetenu(limite);
    Instant fin = pointagesDeLOperateur
      .stream()
      .filter(pointage -> !pointage.isBefore(arrivee) && !pointage.isAfter(limite))
      .filter(pointage -> pointage.isAfter(dernierFait))
      .max(Comparator.naturalOrder())
      .orElse(dernierFait);

    return new JourneeDeTravail(journal, Optional.of(fin));
  }

  /**
   * Abandonnee a cet instant si elle est encore ouverte ; fermee, seulement si son depart suit l'arrivee de plus de
   * 24 h — une borne physique, et non un parametre de l'entreprise, que l'amplitude maximale ne peut jamais atteindre.
   */
  private boolean estPresumeeA(Instant maintenant, Instant limite) {
    return depart()
      .map(date -> date.isAfter(journal.getFirst().dateDeSurvenue().plus(JOURNEE_INVRAISEMBLABLE)))
      .orElseGet(() -> maintenant.isAfter(limite));
  }

  /**
   * Le dernier fait connu d'une venue ouverte, quel qu'il soit. Celui d'une venue fermee se cherche entre l'arrivee et
   * l'arrivee plus le seuil, puisque c'est son depart qu'on ne croit pas.
   */
  private Instant dernierFaitRetenu(Instant limite) {
    if (!estFermee()) {
      return journal.getLast().dateDeSurvenue();
    }

    return journal
      .stream()
      .map(EvenementDePresence::dateDeSurvenue)
      .filter(date -> !date.isAfter(limite))
      .reduce((precedent, suivant) -> suivant)
      .orElseThrow();
  }

  /**
   * Les intervalles ou l'operateur etait present, de chaque arrivee a son depart, dans l'ordre, coupes a la fin
   * presumee : celui qui la franchit, ou reste ouvert, s'y arrete, et ceux qui commencent apres disparaissent.
   */
  public List<Plage> fenetres() {
    return finPresumee
      .map(fin ->
        fenetres(journal)
          .stream()
          .filter(fenetre -> !fenetre.debut().isAfter(fin))
          .map(fenetre -> coupee(fenetre, fin))
          .toList()
      )
      .orElseGet(() -> fenetres(journal));
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

    return new Plage(fenetre.debut(), Optional.of(fin));
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
   * La fin presumee, qui prime sur un depart qu'on ne croit pas, ou a defaut le depart.
   */
  private Optional<Instant> fin() {
    return finPresumee.or(this::depart);
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

  /**
   * Les seuls evenements que l'automate admet, dans l'ordre : un geste refuse est saute, et l'etat reste celui d'avant.
   */
  private static List<EvenementDePresence> retenus(List<EvenementDePresence> evenements) {
    List<EvenementDePresence> retenus = new ArrayList<>();
    EtatDePresence etat = EtatDePresence.ABSENT;

    for (EvenementDePresence evenement : evenements) {
      Optional<EtatDePresence> apres = etat.apres(evenement.type());

      if (apres.isPresent()) {
        retenus.add(evenement);
        etat = apres.orElseThrow();
      }
    }

    return List.copyOf(retenus);
  }

  /**
   * Le journal est deja coherent : chaque transition y est admise.
   */
  private static List<Plage> fenetres(List<EvenementDePresence> evenements) {
    List<Plage> fenetres = new ArrayList<>();
    EtatDePresence etat = EtatDePresence.ABSENT;

    for (int rang = 0; rang < evenements.size(); rang++) {
      EvenementDePresence evenement = evenements.get(rang);
      etat = etat.apres(evenement.type()).orElseThrow();

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

package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * L'interpretation des faits actifs d'une meme cle d'activite — un operateur sur un poste de travail, pour un element
 * engage.
 *
 * <p>
 * Les faits arrivent dans l'ordre du journal, et au plus une activite est en cours sur la cle. Une ouverture termine
 * l'activite en cours a son heure (relance) et en ouvre une nouvelle. Une transition termine l'activite qu'elle vise et
 * ouvre une activite distincte, de l'autre categorie. Une fin termine l'activite qu'elle vise, et elle seule : un geste
 * ne touche jamais une autre activite que sa cible. La cloture, a defaut, ferme l'activite restee en cours.
 * </p>
 *
 * <p>
 * L'echeance gouverne l'interpretation sans aucun instant de lecture, sur les seules heures metier. Une activite que
 * rien n'a terminee a son echeance cesse d'etre en cours pour les faits qui suivent : elle garde sa borne automatique,
 * qu'une relance ne prolonge pas, ni la cloture. Un geste pointe au plus tard a l'echeance de sa cible la termine a son
 * heure ; pointe apres, une fin est conservee sans effet, et une transition ouvre sa nouvelle activite a son heure. Seul
 * un acte du gestionnaire, fin ou transition regularisee, termine une activite au-dela de son echeance.
 * </p>
 *
 * <p>
 * Un geste dont la cible n'est ni l'activite en cours a son heure ni une activite echue — deja terminee, deja remplacee,
 * annulee, ou de la meme categorie que la transition — contredit le journal : il est refuse par
 * {@link TransitionDAtelierInterditeException}.
 * </p>
 */
final class SequenceDActivites {

  private final Map<ActiviteId, Instant> regularisations;
  private final List<Activite> activites = new ArrayList<>();
  private final Map<ActiviteId, Activite> expirees = new HashMap<>();
  private Optional<Activite> courante = Optional.empty();

  private SequenceDActivites(List<EvenementDAtelier> faits) {
    regularisations = faits
      .stream()
      .filter(EvenementDAtelier::estUneRegularisation)
      .filter(fait -> fait.activiteVisee().isPresent())
      .collect(
        Collectors.toMap(
          fait -> fait.activiteVisee().orElseThrow(),
          EvenementDAtelier::dateDeSurvenue,
          BinaryOperator.maxBy(Comparator.naturalOrder())
        )
      );
  }

  static List<Activite> activites(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    SequenceDActivites sequence = new SequenceDActivites(faits);
    faits.forEach(sequence::interprete);
    sequence.clot(cloture);

    return List.copyOf(sequence.activites);
  }

  private void interprete(EvenementDAtelier fait) {
    expireAvant(fait.dateDeSurvenue());
    if (fait.intention() == IntentionDePointage.OUVERTURE) {
      ouvre(fait);
    } else if (fait.intention() == IntentionDePointage.TRANSITION) {
      transite(fait);
    } else {
      arrete(fait);
    }
  }

  /**
   * L'activite courante dont la limite tombe avant l'heure du fait n'est plus vivante : elle garde sa borne
   * automatique, sans fin reelle.
   */
  private void expireAvant(Instant heure) {
    courante
      .filter(activite -> limite(activite).isBefore(heure))
      .ifPresent(activite -> {
        activites.add(activite);
        expirees.put(activite.id(), activite);
        courante = Optional.empty();
      });
  }

  /**
   * Une ouverture termine a son heure l'activite en cours sur la cle, s'il y en a une : c'est la relance.
   */
  private void ouvre(EvenementDAtelier ouverture) {
    courante.ifPresent(activite -> activites.add(activite.termineeA(ouverture.dateDeSurvenue())));
    courante = Optional.of(Activite.ouvertePar(ouverture));
  }

  /**
   * Une transition remplace a son heure l'activite en cours qu'elle vise. Si sa cible a deja expire, sans qu'aucune
   * autre activite ne l'ait suivie, elle n'ouvre que la nouvelle activite, a son heure : la cible garde sa borne
   * automatique.
   */
  private void transite(EvenementDAtelier transition) {
    Optional<Activite> remplacee = cibleCourante(transition)
      .filter(activite -> changeDeCategorie(activite, transition))
      .filter(activite -> peutTerminer(activite, transition));
    if (remplacee.isPresent()) {
      activites.add(remplacee.orElseThrow().termineeA(transition.dateDeSurvenue()));
      courante = Optional.of(Activite.ouvertePar(transition));
      return;
    }

    if (
      courante.isEmpty()
      && cibleExpiree(transition)
        .filter(activite -> changeDeCategorie(activite, transition))
        .isPresent()
    ) {
      courante = Optional.of(Activite.ouvertePar(transition));
      return;
    }

    throw new TransitionDAtelierInterditeException(transition);
  }

  /**
   * Une fin termine a son heure l'activite en cours qu'elle vise. Pointee apres l'echeance de sa cible, elle est
   * conservee sans effet : la cible garde sa borne automatique, ou la fin que le gestionnaire a regularisee.
   */
  private void arrete(EvenementDAtelier fin) {
    Optional<Activite> visee = cibleCourante(fin);
    if (visee.filter(activite -> peutTerminer(activite, fin)).isPresent()) {
      activites.add(visee.orElseThrow().termineeA(fin.dateDeSurvenue()));
      courante = Optional.empty();
      return;
    }

    if (visee.isEmpty() && cibleExpiree(fin).isEmpty()) {
      throw new TransitionDAtelierInterditeException(fin);
    }
  }

  /**
   * La cloture termine a son heure l'activite encore vivante ; elle ne prolonge jamais une activite deja echue.
   */
  private void clot(Optional<Instant> cloture) {
    cloture.ifPresent(this::expireAvant);
    courante.ifPresent(activite -> activites.add(cloture.map(activite::termineeA).orElse(activite)));
  }

  /**
   * L'instant jusqu'auquel une activite reste vivante : son echeance, ou plus tard le dernier geste du gestionnaire qui
   * la termine ou la remplace. Seule une regularisation peut etablir une fin reelle au-dela de l'echeance.
   */
  private Instant limite(Activite activite) {
    Instant echeance = activite.echeance().value();

    return Optional.ofNullable(regularisations.get(activite.id())).filter(echeance::isBefore).orElse(echeance);
  }

  private Optional<Activite> cibleCourante(EvenementDAtelier geste) {
    return courante.filter(activite -> geste.activiteVisee().filter(activite.id()::equals).isPresent());
  }

  private Optional<Activite> cibleExpiree(EvenementDAtelier geste) {
    return geste.activiteVisee().map(expirees::get);
  }

  /**
   * Un geste pointe ne termine sa cible qu'au plus tard a son echeance, un geste exactement a l'echeance l'emportant
   * sur la fin automatique. Seul un acte du gestionnaire peut la terminer au-dela.
   */
  private static boolean peutTerminer(Activite cible, EvenementDAtelier geste) {
    return geste.estUneRegularisation() || !geste.dateDeSurvenue().isAfter(cible.echeance().value());
  }

  private static boolean changeDeCategorie(Activite activite, EvenementDAtelier transition) {
    return activite.ouvrant().type() != transition.type();
  }
}

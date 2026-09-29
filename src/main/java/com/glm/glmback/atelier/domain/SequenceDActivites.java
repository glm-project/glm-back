package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
 * Un geste dont la cible n'est pas l'activite en cours a son heure — deja terminee, deja remplacee, annulee, ou de la
 * meme categorie que la transition — contredit le journal : il est refuse par
 * {@link TransitionDAtelierInterditeException}.
 * </p>
 */
final class SequenceDActivites {

  private SequenceDActivites() {}

  static List<Activite> activites(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    List<Activite> activites = new ArrayList<>();
    Optional<Activite> enCours = Optional.empty();

    for (EvenementDAtelier fait : faits) {
      if (fait.intention().viseUneActivite() && !termine(enCours, fait)) {
        throw new TransitionDAtelierInterditeException(fait);
      }

      enCours.ifPresent(activite -> activites.add(activite.termineeA(fait.dateDeSurvenue())));
      enCours = fait.intention().ouvreUneActivite() ? Optional.of(Activite.ouvertePar(fait)) : Optional.empty();
    }
    enCours.ifPresent(activite -> activites.add(cloture.map(activite::termineeA).orElse(activite)));

    return activites;
  }

  /**
   * Vrai si le geste vise l'activite en cours et peut la terminer : toujours pour une fin, pour une transition
   * seulement vers l'autre categorie.
   */
  private static boolean termine(Optional<Activite> enCours, EvenementDAtelier geste) {
    return enCours
      .filter(activite -> geste.activiteVisee().filter(activite.id()::equals).isPresent())
      .filter(activite -> geste.intention() == IntentionDePointage.FIN || activite.ouvrant().type() != geste.type())
      .isPresent();
  }
}

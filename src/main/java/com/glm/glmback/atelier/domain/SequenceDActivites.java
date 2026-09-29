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

  static List<IntervalleDActivite> intervalles(List<EvenementDAtelier> faits, Optional<Instant> fermetureFinale) {
    List<IntervalleDActivite> intervalles = new ArrayList<>();
    Optional<EvenementDAtelier> enCours = Optional.empty();

    for (EvenementDAtelier fait : faits) {
      if (fait.intention().viseUneActivite() && !termine(enCours, fait)) {
        throw new TransitionDAtelierInterditeException(fait);
      }

      enCours.ifPresent(ouvrant -> intervalles.add(intervalle(ouvrant, Optional.of(fait.dateDeSurvenue()))));
      enCours = fait.intention().ouvreUneActivite() ? Optional.of(fait) : Optional.empty();
    }
    enCours.ifPresent(ouvrant -> intervalles.add(intervalle(ouvrant, fermetureFinale)));

    return intervalles;
  }

  /**
   * Vrai si le geste vise l'activite en cours et peut la terminer : toujours pour une fin, pour une transition
   * seulement vers l'autre categorie.
   */
  private static boolean termine(Optional<EvenementDAtelier> enCours, EvenementDAtelier geste) {
    return enCours
      .filter(ouvrant -> ouvrant.activite().equals(geste.activiteVisee()))
      .filter(ouvrant -> geste.intention() == IntentionDePointage.FIN || ouvrant.type() != geste.type())
      .isPresent();
  }

  private static IntervalleDActivite intervalle(EvenementDAtelier ouvrant, Optional<Instant> fin) {
    return IntervalleDActivite.builder()
      .evenement(ouvrant.id())
      .operateur(ouvrant.operateur())
      .poste(ouvrant.poste())
      .nature(ouvrant.nature())
      .categorie(ouvrant.type().categorie().orElseThrow())
      .debut(ouvrant.dateDeSurvenue())
      .fin(fin);
  }
}

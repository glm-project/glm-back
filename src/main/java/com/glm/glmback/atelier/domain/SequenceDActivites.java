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
 * Les faits arrivent dans l'ordre du journal. L'automate {@link EtatDActivite} les deroule : chaque fait qui laisse
 * l'activite ouverte commence un intervalle, ferme par le fait suivant de la meme cle ou, a defaut, par la fermeture
 * finale. Un fait que l'automate n'admet pas est refuse par {@link TransitionDAtelierInterditeException}.
 * </p>
 */
final class SequenceDActivites {

  private SequenceDActivites() {}

  static List<IntervalleDActivite> intervalles(List<EvenementDAtelier> faits, Optional<Instant> fermetureFinale) {
    List<IntervalleDActivite> intervalles = new ArrayList<>();
    EtatDActivite etat = EtatDActivite.ABSENTE;

    for (int rang = 0; rang < faits.size(); rang++) {
      EvenementDAtelier fait = faits.get(rang);
      EtatDActivite avant = etat;
      etat = avant.apres(fait.type()).orElseThrow(() -> new TransitionDAtelierInterditeException(fait, avant));

      Optional<Instant> fin = rang + 1 < faits.size() ? Optional.of(faits.get(rang + 1).dateDeSurvenue()) : fermetureFinale;
      etat.categorie().ifPresent(categorie -> intervalles.add(intervalle(fait, categorie, fin)));
    }

    return intervalles;
  }

  private static IntervalleDActivite intervalle(EvenementDAtelier fait, CategorieDActivite categorie, Optional<Instant> fin) {
    return IntervalleDActivite.builder()
      .evenement(fait.id())
      .operateur(fait.operateur())
      .poste(fait.poste())
      .nature(fait.nature())
      .categorie(categorie)
      .debut(fait.dateDeSurvenue())
      .fin(fin);
  }
}

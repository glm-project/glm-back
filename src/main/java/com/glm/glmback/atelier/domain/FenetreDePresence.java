package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un intervalle pendant lequel un operateur etait present et non en pause.
 *
 * <p>
 * C'est la matiere du temps effectif : le temps passe sur un element ne vaut que dans les fenetres de presence de
 * l'operateur, ce qui fait qu'une pause de midi scinde son travail et qu'un depart le referme, sans qu'aucun de ces
 * deux faits ait eu besoin d'etre recopie dans le journal de l'element.
 * </p>
 */
public record FenetreDePresence(Instant debut, Optional<Instant> fin) {
  public FenetreDePresence {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }

  public boolean estOuverte() {
    return fin.isEmpty();
  }

  /**
   * La part de l'intervalle donne qui tombe dans cette fenetre, s'il y en a une.
   *
   * <p>
   * Une intersection reduite a un instant ne rend rien : une pause prise a la seconde ou le travail commence ne
   * produit pas d'intervalle de duree nulle.
   * </p>
   */
  public Optional<FenetreDePresence> intersection(Instant autreDebut, Optional<Instant> autreFin) {
    Instant debutCommun = debut.isAfter(autreDebut) ? debut : autreDebut;
    Optional<Instant> finCommune = plusTot(fin, autreFin);

    if (finCommune.filter(date -> !date.isAfter(debutCommun)).isPresent()) {
      return Optional.empty();
    }

    return Optional.of(new FenetreDePresence(debutCommun, finCommune));
  }

  private static Optional<Instant> plusTot(Optional<Instant> une, Optional<Instant> autre) {
    return une.map(date -> autre.filter(date::isAfter).orElse(date)).or(() -> autre);
  }
}

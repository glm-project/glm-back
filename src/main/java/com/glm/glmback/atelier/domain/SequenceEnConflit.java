package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/**
 * Des pointages d'un meme operateur sur un meme poste qui se contredisent, et les activites que leur contradiction
 * laisse a resoudre.
 *
 * <p>
 * Elle se deduit des faits actifs, jamais d'un etat stocke : le gestionnaire la resout en corrigeant ou en annulant
 * les faits concernes, et elle disparait au recalcul des que les faits redeviennent coherents. Tant qu'elle dure, ses
 * activites ne sont ni en cours ni terminees, et aucune fin automatique ne tranche a leur place.
 * </p>
 *
 * <p>
 * Ses pointages sont ceux qui ouvrent ou visent ses activites, et les gestes contradictoires eux-memes, y compris
 * celui qui vise une ouverture annulee et ne laisse donc aucune activite a resoudre.
 * </p>
 */
public record SequenceEnConflit(CleDActivite cle, List<ActiviteId> activites, List<EvenementDAtelierId> pointages) {
  public SequenceEnConflit {
    Assert.notNull("cle", cle);
    Assert.field("activites", activites).notNull().noNullElement();
    Assert.field("pointages", pointages).notEmpty().noNullElement();
    activites = List.copyOf(activites);
    pointages = List.copyOf(pointages);
  }

  public OperateurId operateur() {
    return cle.operateur();
  }

  public Optional<PosteDeTravailId> poste() {
    return cle.poste();
  }
}

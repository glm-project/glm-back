package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Un pointage du pupitre que son horloge date trop loin en avance sur le serveur : il n'est pas juge, et part en audit
 * avec l'heure qu'il annonce et l'heure ou le serveur l'a recu.
 *
 * <p>
 * Ce n'est pas un pointage ignore par la regle de reception : il n'a ete compare a aucun pointage accepte, et son heure
 * annoncee est posterieure a sa reception, ce qu'aucun {@link Horodatage} ne porte. L'audit le garde pour que le
 * gestionnaire retrouve le geste et le rattrape par un ajout d'activite ; le pupitre, lui, recoit le refus
 * {@code date-de-survenue-future}.
 * </p>
 */
public record PointageEnAvance(PointageAEnregistrer pointage, Instant dateDeReception) {
  public PointageEnAvance {
    Assert.notNull("pointage", pointage);
    Assert.notNull("dateDeReception", dateDeReception);
    Assert.field("dateDeSurvenue", pointage.dateDeSurvenue().orElse(null)).after(dateDeReception);
  }

  /**
   * L'heure du geste telle que le pupitre l'annonce.
   */
  public Instant dateDeSurvenue() {
    return pointage.dateDeSurvenue().orElseThrow();
  }

  public RaisonDePointageIgnore raison() {
    return RaisonDePointageIgnore.HORLOGE_EN_AVANCE;
  }
}

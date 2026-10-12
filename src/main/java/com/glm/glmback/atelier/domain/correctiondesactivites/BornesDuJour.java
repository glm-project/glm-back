package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Les minuits du jour lu, calcules dans le fuseau de l'entreprise : le debut du jour (inclus), sa fin (exclue, minuit du
 * lendemain) et la fin du lendemain, que la fin d'une activite ajoutee ne depasse pas. Le client ne calcule aucun minuit.
 */
public record BornesDuJour(Instant debut, Instant fin, Instant finDuLendemain) {
  public BornesDuJour {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    Assert.notNull("finDuLendemain", finDuLendemain);
  }
}

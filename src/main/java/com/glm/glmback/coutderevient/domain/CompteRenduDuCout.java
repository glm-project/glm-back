package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.ZoneId;

/**
 * Le rapport tel qu'il part chez le client : les memes montants que l'ecran, l'avancement de l'element et le fuseau
 * dans lequel l'entreprise lit ses heures.
 *
 * <p>
 * Rien n'y est recalcule ni arrondi a nouveau : un export ne fait que mettre en forme le {@link CoutDeRevient}.
 * </p>
 */
public record CompteRenduDuCout(CoutDeRevient rapport, StatutDeLElement statut, ZoneId fuseau) {
  public CompteRenduDuCout {
    Assert.notNull("rapport", rapport);
    Assert.notNull("statut", statut);
    Assert.notNull("fuseau horaire", fuseau);
  }
}

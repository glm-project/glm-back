package com.glm.glmback.coutderevient.domain;

import java.util.List;

/**
 * Les passages en atelier d'un element, journaux compris.
 *
 * <p>
 * Plusieurs, et non un seul : un element reengage apres cloture a plusieurs suivis, et son cout de revient les
 * additionne.
 * </p>
 */
@FunctionalInterface
public interface TravailDeLElement {
  List<SuiviDuTravail> suivis(ElementId element);
}

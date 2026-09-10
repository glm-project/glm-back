package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Un pointage tel qu'affiche dans le releve : le type et l'instant restent ceux du journal, {@code valide} dit si
 * son enchainement avec ce qui precede respecte l'automate de presence.
 *
 * <p>
 * Un pointage fautif n'est jamais ecarte du releve — il reste visible pour que le gestionnaire sache qu'une
 * correction est attendue — mais il n'entre pour rien dans le calcul des fenetres de presence.
 * </p>
 */
public record Pointage(EvenementDePresence evenement, boolean valide) {
  public Pointage {
    Assert.notNull("evenement", evenement);
  }
}

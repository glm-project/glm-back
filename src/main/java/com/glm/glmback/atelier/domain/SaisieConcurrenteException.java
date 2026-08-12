package com.glm.glmback.atelier.domain;

/**
 * Une saisie a ete calculee sur un journal que quelqu'un d'autre a complete entre temps.
 *
 * <p>
 * Le journal est relu, replie, puis reecrit a chaque pointage : deux saisies parties du meme etat valident chacune sa
 * transition contre un journal qui ignore l'autre. Rien ne serait perdu — le rapprochement est additif — mais la
 * sequence obtenue n'aurait ete validee par personne, et un journal portant deux debuts consecutifs sur la meme
 * activite deviendrait illisible : l'automate le refuserait a chaque relecture. On refuse donc la seconde saisie, a
 * charge pour l'appelant de la rejouer sur l'etat rafraichi.
 * </p>
 */
public final class SaisieConcurrenteException extends RuntimeException {

  public SaisieConcurrenteException(SuiviDAtelierId id) {
    super("Le suivi d'atelier %s a ete modifie par une autre saisie".formatted(id.uuid()));
  }

  public SaisieConcurrenteException(JourneeDeTravailId id) {
    super("La journee de travail %s a ete modifiee par une autre saisie".formatted(id.uuid()));
  }
}

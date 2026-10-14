package com.glm.glmback.atelier.domain;

import java.time.Instant;

/**
 * Une activite ajoutee, ou dont le poste change, qui chevauche une autre activite du meme operateur sur le meme poste,
 * dans le meme suivi. Deux activites de cles differentes peuvent se chevaucher ; une meme cle, non.
 */
public final class ChevauchementException extends RuntimeException {

  public ChevauchementException(CleDActivite cle, Instant debut, Instant fin) {
    super(
      "L'activite de l'operateur %s sur le poste %s, du %s au %s, chevauche une autre activite de la meme cle".formatted(
        cle.operateur().uuid(),
        cle
          .poste()
          .map(poste -> poste.uuid().toString())
          .orElse("(aucun)"),
        debut,
        fin
      )
    );
  }
}

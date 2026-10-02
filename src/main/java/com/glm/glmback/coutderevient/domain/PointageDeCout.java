package com.glm.glmback.coutderevient.domain;

import java.time.Instant;

/**
 * Un pointage d'une ligne, tel que le detail le justifie : chiffre s'il est termine, sans montant s'il est a resoudre.
 *
 * <p>
 * Les deux formes different par ce qu'elles savent : un pointage valorise a une fin et des parts chiffrees, un
 * pointage a resoudre n'a qu'une fin au plus tard et les pointages contradictoires qui l'empechent d'en avoir une.
 * </p>
 */
public sealed interface PointageDeCout permits PointageValorise, PointageAResoudre {
  Activite activite();

  Instant debut();
}

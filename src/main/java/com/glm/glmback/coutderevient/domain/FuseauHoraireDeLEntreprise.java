package com.glm.glmback.coutderevient.domain;

import java.time.ZoneId;

/**
 * Le fuseau horaire dans lequel l'entreprise lit ses heures.
 *
 * <p>
 * C'est une donnee de parametrage, donc un port : un compte rendu ecrit en UTC montrerait 05:30 pour un pointage de
 * 07:30 a Paris.
 * </p>
 */
public interface FuseauHoraireDeLEntreprise {
  ZoneId zone();
}

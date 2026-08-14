package com.glm.glmback.feuilledetemps.domain;

import java.time.ZoneId;

/**
 * Le fuseau horaire dans lequel l'entreprise lit ses jours.
 *
 * <p>
 * C'est une donnee de parametrage, donc un port : le domaine ne code pas une zone en constante, sans quoi une
 * entreprise cliente d'un autre fuseau verrait ses journees coupees a la mauvaise heure.
 * </p>
 */
public interface FuseauHoraireDeLEntreprise {
  ZoneId zone();
}

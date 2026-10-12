package com.glm.glmback.atelier.domain.correctiondesactivites;

import java.time.ZoneId;

/**
 * Le fuseau horaire dans lequel l'entreprise lit ses jours.
 *
 * <p>
 * C'est une donnee de parametrage, donc un port : le domaine ne code pas une zone en constante. Seule la lecture de la
 * journee a corriger l'utilise, pour dire ou tombent les minuits ; le coeur de l'atelier, lui, ne connait ni fuseau ni
 * jour calendaire. Le port est le jumeau de ceux de la feuille de temps, de la synthese des heures et du cout de revient,
 * chaque contexte gardant le sien.
 * </p>
 */
public interface FuseauHoraireDeLEntreprise {
  ZoneId zone();
}

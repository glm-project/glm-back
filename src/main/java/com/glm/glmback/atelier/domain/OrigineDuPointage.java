package com.glm.glmback.atelier.domain;

/**
 * L'acte qui a porte un fait au journal d'atelier.
 *
 * <p>
 * Tout ce qui passe par la route des pointages est un {@link #POINTAGE}, quels que soient le role de celui qui pointe
 * et l'ecart entre l'heure du fait et celle de sa saisie : un pupitre reste hors ligne rejoue ses gestes apres coup,
 * sans que cela en fasse un acte du gestionnaire. Une {@link #REGULARISATION} est l'acte explicite du gestionnaire qui
 * etablit un fait oublie. L'origine se conserve donc sur
 * le fait : ce qui est reserve au gestionnaire ne se deduit pas des dates.
 * </p>
 */
public enum OrigineDuPointage {
  POINTAGE,
  REGULARISATION,
}

package com.glm.glmback.atelier.domain;

/**
 * L'acte qui a porte un fait au journal d'atelier.
 *
 * <p>
 * Tout ce qui passe par la route des pointages est un {@link #POINTAGE}, quels que soient le role de celui qui pointe
 * et l'ecart entre l'heure du fait et celle de sa saisie : un pupitre reste hors ligne rejoue ses gestes apres coup,
 * sans que cela en fasse un acte du gestionnaire. Une {@link #REGULARISATION} est tout fait que le gestionnaire ecrit : la fin
 * qu'il regularise, l'activite oubliee qu'il ajoute (ouverture et fin), le retrait d'une activite, le changement de son
 * poste. L'origine se conserve donc sur le fait : ce qui est reserve au gestionnaire ne se deduit pas des dates.
 * </p>
 */
public enum OrigineDuPointage {
  POINTAGE,
  REGULARISATION,
}

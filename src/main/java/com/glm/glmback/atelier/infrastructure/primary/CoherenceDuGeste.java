package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.util.UUID;

/**
 * Les deux regles qu'un corps de geste d'atelier doit respecter pour etre lu, telles que le domaine les pose : le
 * pointage, la regularisation et la correction les partagent.
 *
 * <p>
 * Un champ absent est laisse a son propre {@code @NotNull} : il ne fait pas, en plus, une combinaison invalide.
 * </p>
 */
public final class CoherenceDuGeste {

  public static final String INTENTION_ADMISE =
    "une fin se pointe avec l'intention FIN, un debut ou une non conformite avec OUVERTURE ou TRANSITION";
  public static final String CIBLE_CONFORME = "une transition ou une fin porte la cible qu'elle vise, une ouverture n'en porte aucune";

  private CoherenceDuGeste() {}

  public static boolean intentionAdmise(TypeDEvenementDAtelier type, IntentionDePointage intention) {
    return type == null || intention == null || intention.admet(type);
  }

  public static boolean cibleConforme(IntentionDePointage intention, UUID cible) {
    return intention == null || intention.viseUneActivite() == (cible != null);
  }
}

package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;

/**
 * Tout ce que le pupitre met en cache pour continuer a fonctionner sans reseau, et la date de cet instantane.
 *
 * <p>
 * {@code genereLe} est la version : elle dit de quand date ce que l'ecran d'atelier affiche, ce qu'un indicateur
 * binaire connecte / hors ligne ne dit pas. Deux lectures sans changement rendent deux dates differentes, et c'est
 * voulu — dater le dernier changement supposerait d'horodater les modifications des referentiels voisins.
 * </p>
 */
public record ReferentielDuPupitre(Instant genereLe, List<OperateurDuPupitre> operateurs, List<SuiviDuPupitre> suivis) {
  public ReferentielDuPupitre {
    Assert.notNull("date de generation", genereLe);
    Assert.field("operateurs", operateurs).notNull().noNullElement();
    Assert.field("suivis", suivis).notNull().noNullElement();
    operateurs = List.copyOf(operateurs);
    suivis = List.copyOf(suivis);
  }
}

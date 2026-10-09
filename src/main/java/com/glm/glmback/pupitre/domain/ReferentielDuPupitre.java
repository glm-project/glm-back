package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
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
 *
 * <p>
 * Il porte aussi la duree maximale d'une activite, que le pupitre hors ligne lit au lieu de la coder : c'est celle dont
 * l'atelier tire l'echeance de chaque activite.
 * </p>
 */
public record ReferentielDuPupitre(
  Instant genereLe,
  List<OperateurDuPupitre> operateurs,
  List<SuiviDuPupitre> suivis,
  List<CategorieDElement> categories,
  MaximumActivityDuration dureeMaximaleDActivite
) {
  public ReferentielDuPupitre {
    Assert.notNull("date de generation", genereLe);
    Assert.field("operateurs", operateurs).notNull().noNullElement();
    Assert.field("suivis", suivis).notNull().noNullElement();
    Assert.field("categories", categories).notNull().noNullElement();
    Assert.notNull("duree maximale d'activite", dureeMaximaleDActivite);
    operateurs = List.copyOf(operateurs);
    suivis = List.copyOf(suivis);
    categories = List.copyOf(categories);
  }
}

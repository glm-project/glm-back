package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;
import java.util.List;

/**
 * Un jour du calendrier, la presence de l'operateur et le travail qu'il y a pointe, element par element.
 *
 * <p>
 * Un jour sans presence n'est pas absent de la feuille : les sept jours sont toujours rendus, sans quoi le lecteur
 * devrait deduire d'un trou si l'operateur etait en conge ou si la semaine n'est pas encore finie.
 * </p>
 */
public record JourDeLaSemaine(LocalDate jour, List<Plage> presence, List<IntervalleDActivite> activites) {
  public JourDeLaSemaine {
    Assert.notNull("jour", jour);
    Assert.field("presence", presence).notNull().noNullElement();
    Assert.field("activites", activites).notNull().noNullElement();
  }
}

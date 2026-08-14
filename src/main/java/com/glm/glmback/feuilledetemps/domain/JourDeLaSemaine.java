package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;
import java.util.List;

/**
 * Un jour du calendrier et ce que l'operateur y a passe.
 *
 * <p>
 * Un jour sans presence n'est pas absent de la feuille : les sept jours sont toujours rendus, sans quoi le lecteur
 * devrait deduire d'un trou si l'operateur etait en conge ou si la semaine n'est pas encore finie.
 * </p>
 */
public record JourDeLaSemaine(LocalDate jour, List<Plage> presence) {
  public JourDeLaSemaine {
    Assert.notNull("jour", jour);
    Assert.field("presence", presence).notNull().noNullElement();
  }
}

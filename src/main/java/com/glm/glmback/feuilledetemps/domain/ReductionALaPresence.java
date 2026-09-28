package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Le travail de l'operateur ramene a ses fenetres de presence.
 *
 * <p>
 * Meme regle que celle de l'atelier, rejouee ici : chaque intervalle est intersecte avec les fenetres de
 * <strong>la journee ou il a commence</strong>, lue comme la presence (fin presumee comprise). Un depart referme ce
 * que l'operateur a oublie d'arreter, et un travail croise avec une fenetre presumee devient presume.
 * </p>
 *
 * <p>
 * La journee qui contient un instant est, comme dans l'atelier, la plus recente dont l'arrivee precede l'instant et
 * qui n'est pas finie avant lui. Un debut qui ne tombe dans aucune journee est <strong>ecarte</strong>, la ou
 * l'atelier le rend intact : ce contexte repond a « qu'a fait cette personne cette semaine », et sans presence aucun
 * jour ne peut l'accueillir sans arbitraire.
 * </p>
 */
public record ReductionALaPresence(List<JourneeDeTravail> journees) {
  private static final Comparator<JourneeDeTravail> PAR_ARRIVEE = Comparator.comparing(journee -> journee.arrivee().orElseThrow());

  public ReductionALaPresence {
    Assert.field("journees", journees).notNull().noNullElement();
  }

  public List<IntervalleDActivite> reduit(IntervalleDActivite intervalle) {
    return journeeContenant(intervalle)
      .map(journee ->
        journee
          .fenetres()
          .stream()
          .flatMap(fenetre -> intervalle.reduitA(fenetre).stream())
          .toList()
      )
      .orElseGet(List::of);
  }

  private Optional<JourneeDeTravail> journeeContenant(IntervalleDActivite intervalle) {
    return journees
      .stream()
      .filter(journee -> journee.contient(intervalle.plage().debut()))
      .max(PAR_ARRIVEE);
  }
}

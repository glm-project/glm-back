package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Le calendrier de l'entreprise, vu de la lecture de la journee : ou tombent ses minuits, et comment une plage se coupe
 * a chacun. Minuit n'existe qu'une fois la zone connue, et c'est lui qui decide a quel jour appartient une heure de
 * travail.
 */
final class DecoupageParJour {

  private final ZoneId zone;

  DecoupageParJour(ZoneId zone) {
    Assert.notNull("fuseau horaire", zone);

    this.zone = zone;
  }

  Instant debutDe(LocalDate jour) {
    return jour.atStartOfDay(zone).toInstant();
  }

  /**
   * La plage coupee a chaque minuit qu'elle traverse, jour par jour. Une plage vide n'a aucune portion.
   */
  List<PortionDeJour> portions(Instant debut, Instant fin) {
    List<PortionDeJour> portions = new ArrayList<>();
    LocalDate jour = LocalDate.ofInstant(debut, zone);
    Instant curseur = debut;

    while (curseur.isBefore(fin)) {
      Instant minuitSuivant = debutDe(jour.plusDays(1));
      Instant borne = minuitSuivant.isBefore(fin) ? minuitSuivant : fin;
      portions.add(new PortionDeJour(jour, Duration.between(curseur, borne)));
      curseur = borne;
      jour = jour.plusDays(1);
    }

    return List.copyOf(portions);
  }
}

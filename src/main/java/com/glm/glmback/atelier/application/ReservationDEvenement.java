package com.glm.glmback.atelier.application;

import java.util.Optional;

/** Une reservation inedite ne porte pas encore son agregat ; un rejeu, si. */
public record ReservationDEvenement(Optional<AgregatDEvenement> agregat) {
  public static ReservationDEvenement inedite() {
    return new ReservationDEvenement(Optional.empty());
  }

  public static ReservationDEvenement rejeu(AgregatDEvenement agregat) {
    return new ReservationDEvenement(Optional.of(agregat));
  }

  public boolean estUnRejeu() {
    return agregat.isPresent();
  }
}

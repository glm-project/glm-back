package com.glm.glmback.atelier.application;

import java.util.UUID;

/** Registre durable et atomique des identites des evenements d'atelier. */
public interface IdentitesDEvenements {
  ReservationDEvenement reserve(UUID evenement, EmpreinteDEvenement empreinte);

  void associe(UUID evenement, AgregatDEvenement agregat);
}

package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.MoisCalendaire;
import com.glm.glmback.syntheseheures.domain.PeriodeCalendaire;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** La periode d'un rapport telle que l'URL la demande : une annee et soit une semaine ISO, soit un mois civil. */
final class PeriodeDemandee {

  private PeriodeDemandee() {}

  static PeriodeCalendaire de(int annee, Optional<Integer> semaine, Optional<Integer> mois) {
    if (semaine.isPresent() == mois.isPresent()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La periode se demande par une semaine ou par un mois, et un seul.");
    }

    return semaine
      .<PeriodeCalendaire>map(numero -> new SemaineCalendaire(annee, numero))
      .orElseGet(() -> new MoisCalendaire(annee, mois.orElseThrow()));
  }
}

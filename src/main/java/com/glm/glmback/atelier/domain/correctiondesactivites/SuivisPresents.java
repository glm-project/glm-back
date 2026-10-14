package com.glm.glmback.atelier.domain.correctiondesactivites;

import java.time.Instant;
import java.util.List;

/**
 * Les suivis de l'entreprise presents a l'atelier sur une plage : engages avant sa fin et non clotures avant son debut,
 * y compris clotures depuis. Ils sont rendus par engagement, puis par identite.
 */
public interface SuivisPresents {
  List<SuiviPresent> entre(Instant debut, Instant finExclue);
}

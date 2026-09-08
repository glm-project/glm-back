package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Un passage d'un element en atelier : son journal, et la cloture qui le referme s'il y en a une.
 *
 * <p>
 * Un element reengage apres cloture en a plusieurs, et son cout de revient les additionne : c'est ce que change
 * l'entree par l'element plutot que par le suivi.
 * </p>
 */
public record SuiviDuTravail(JournalDAtelier journal, Optional<Instant> cloture) {
  public SuiviDuTravail {
    Assert.notNull("journal", journal);
    Assert.notNull("cloture", cloture);
  }

  public List<IntervalleDActivite> intervalles() {
    return journal.intervalles(cloture);
  }
}

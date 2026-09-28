package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Un passage d'un element en atelier, vu depuis l'operateur : l'element engage, les pointages de l'operateur sur ce
 * passage, et la cloture qui le referme s'il y en a une.
 *
 * <p>
 * Le journal est entier, et non restreint a la semaine : une non conformite de la semaine peut suivre un debut de la
 * semaine precedente, et c'est ce debut qui dit sur quoi elle porte.
 * </p>
 */
public record SuiviDuTravail(ElementEngage element, JournalDAtelier journal, Optional<Instant> cloture) {
  public SuiviDuTravail {
    Assert.notNull("element", element);
    Assert.notNull("journal", journal);
    Assert.notNull("cloture", cloture);
  }

  public List<IntervalleDActivite> intervalles() {
    return journal.intervalles(element.id(), cloture);
  }

  /**
   * Le journal brut de l'operateur sur cet element, dans l'ordre du journal : tous ses pointages, y compris ceux que
   * l'automate ignore.
   */
  public List<PointageDElement> pointages() {
    return journal
      .pointages()
      .stream()
      .map(pointage ->
        PointageDElement.builder()
          .type(pointage.type())
          .element(element.id())
          .poste(pointage.poste())
          .nature(pointage.nature())
          .dateDeSurvenue(pointage.dateDeSurvenue())
      )
      .toList();
  }
}

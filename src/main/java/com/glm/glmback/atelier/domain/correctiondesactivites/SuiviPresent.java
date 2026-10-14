package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un suivi present a l'atelier le jour lu : engage avant la fin du jour et non cloture avant son debut, y compris
 * cloture depuis. Il porte son engagement, et sa cloture quand il en a une, pour distinguer deux entrees du meme element
 * et verifier une plage.
 */
public record SuiviPresent(SuiviDAtelierId id, ElementEngage element, Instant engageLe, Optional<Instant> clotureLe) {
  public SuiviPresent {
    Assert.notNull("suivi", id);
    Assert.notNull("element", element);
    Assert.notNull("engageLe", engageLe);
    Assert.notNull("clotureLe", clotureLe);
  }

  public static SuiviPresentIdBuilder builder() {
    return id -> element -> engageLe -> clotureLe -> new SuiviPresent(id, element, engageLe, clotureLe);
  }

  public interface SuiviPresentIdBuilder {
    SuiviPresentElementBuilder id(SuiviDAtelierId id);
  }

  public interface SuiviPresentElementBuilder {
    SuiviPresentEngageLeBuilder element(ElementEngage element);
  }

  public interface SuiviPresentEngageLeBuilder {
    SuiviPresentClotureLeBuilder engageLe(Instant engageLe);
  }

  public interface SuiviPresentClotureLeBuilder {
    SuiviPresent clotureLe(Optional<Instant> clotureLe);
  }
}

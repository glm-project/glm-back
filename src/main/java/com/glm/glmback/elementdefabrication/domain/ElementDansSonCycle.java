package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Un element de fabrication et ce que son suivi d'atelier en dit : la lecture des ecrans, jamais un agregat.
 */
public record ElementDansSonCycle(ElementDeFabrication element, Optional<PassageEnAtelier> passage) {
  public ElementDansSonCycle {
    Assert.notNull("element", element);
    Assert.notNull("passage", passage);
  }

  public CycleDeVie cycle() {
    return passage
      .map(lu ->
        switch (lu) {
          case PassageOuvert ouvert -> CycleDeVie.EN_ATELIER;
          case PassageClos clos -> CycleDeVie.CLOTURE;
        }
      )
      .orElse(CycleDeVie.EN_PREPARATION);
  }
}

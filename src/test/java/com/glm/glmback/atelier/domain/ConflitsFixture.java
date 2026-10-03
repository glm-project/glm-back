package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

import java.time.Instant;
import java.util.UUID;

public final class ConflitsFixture {

  private ConflitsFixture() {}

  public static SuiviDAtelier suiviOF2026000042EngageA(Instant debut) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, TypeDElementEngage.ORDRE_DE_FABRICATION))
      .engagement(new Engagement(AUTEUR_LEROY, debut.minusSeconds(3600)))
      .journal(JournalDAtelier.vide());
  }
}

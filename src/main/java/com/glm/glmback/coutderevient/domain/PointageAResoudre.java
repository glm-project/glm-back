package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un pointage qu'une sequence en conflit empeche de borner : ni duree ni montant, seulement son debut et sa fin au
 * plus tard.
 */
public record PointageAResoudre(ActiviteInterpretee interpretee) implements PointageDeCout {
  public PointageAResoudre {
    Assert.notNull("activite a resoudre", interpretee);
  }

  @Override
  public Activite activite() {
    return interpretee.activite();
  }

  @Override
  public Instant debut() {
    return interpretee.plage().debut();
  }

  public Optional<Instant> finAuPlusTard() {
    return interpretee.finAuPlusTard();
  }
}

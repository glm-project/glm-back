package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Un pointage qu'une sequence en conflit empeche de borner : ni duree ni montant, seulement son debut, sa fin au plus
 * tard et les pointages contradictoires de ses sequences, que le gestionnaire doit trancher.
 */
public record PointageAResoudre(ActiviteInterpretee interpretee, List<PointageEnConflit> contradictoires) implements PointageDeCout {
  public PointageAResoudre {
    Assert.notNull("activite a resoudre", interpretee);
    Assert.field("pointages contradictoires", contradictoires).notNull().noNullElement();
    contradictoires = List.copyOf(contradictoires);
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

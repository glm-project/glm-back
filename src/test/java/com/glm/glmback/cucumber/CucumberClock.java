package com.glm.glmback.cucumber;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Une horloge que les scenarios peuvent figer.
 *
 * <p>
 * Sans elle, la moitie des scenarios d'atelier n'aurait aucun sens : un pointage se date toujours sur l'instant
 * present, et c'est justement l'ecart entre l'heure du fait et celle de sa saisie que le contexte donne a voir. Tant
 * qu'aucun scenario ne la fige, elle rend l'heure du systeme.
 * </p>
 */
public class CucumberClock extends java.time.Clock {

  private final AtomicReference<Instant> fige = new AtomicReference<>();

  public void ilEst(Instant instant) {
    fige.set(instant);
  }

  public void reset() {
    fige.set(null);
  }

  @Override
  public Instant instant() {
    return Optional.ofNullable(fige.get()).orElseGet(Instant::now);
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public java.time.Clock withZone(ZoneId zone) {
    return this;
  }
}

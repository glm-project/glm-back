package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Un pointage termine et tout ce qui le chiffre : la machine sur la tranche entiere, la main d'oeuvre part par part,
 * une part par fenetre de partage traversee.
 */
public record PointageValorise(TrancheDActivite tranche, List<TrancheValorisable> parts) implements PointageDeCout {
  public PointageValorise {
    Assert.notNull("tranche", tranche);
    Assert.field("parts", parts).notNull().noNullElement();
    parts = List.copyOf(parts);
  }

  @Override
  public Activite activite() {
    return tranche.activite();
  }

  @Override
  public Instant debut() {
    return tranche.periode().debut();
  }

  public Montant machine() {
    return tranche.coutMachine();
  }

  /**
   * La somme de ses parts, deja au centime ; rien des qu'une part attend un diviseur inconnu.
   */
  public Optional<Montant> mainDOeuvre() {
    if (partageInconnu()) {
      return Optional.empty();
    }
    return Optional.of(
      parts
        .stream()
        .map(part -> part.coutDeMainDOeuvre().orElseThrow())
        .reduce(Montant.ZERO, Montant::plus)
    );
  }

  public boolean finAutomatique() {
    return tranche.finAutomatique();
  }

  /**
   * Le pointage est correct, mais un pointage a resoudre de son operateur, sur un autre poste, empeche de savoir
   * comment partager une part de son temps.
   */
  public boolean partageInconnu() {
    return parts.stream().anyMatch(part -> part.coutDeMainDOeuvre().isEmpty());
  }
}

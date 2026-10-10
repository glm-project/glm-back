package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;

/**
 * Un pointage termine et tout ce qui le chiffre : la machine sur la tranche entiere, la main d'oeuvre part par part,
 * une part par fenetre de partage traversee.
 */
public record PointageValorise(TrancheDActivite tranche, List<TrancheValorisable> parts) {
  public PointageValorise {
    Assert.notNull("tranche", tranche);
    Assert.field("parts", parts).notNull().noNullElement();
    parts = List.copyOf(parts);
  }

  public Activite activite() {
    return tranche.activite();
  }

  public Instant debut() {
    return tranche.periode().debut();
  }

  public Instant fin() {
    return tranche.periode().fin();
  }

  public DureeTotale duree() {
    return DureeTotale.de(tranche.duree());
  }

  public Cout cout() {
    return new Cout(machine(), mainDOeuvre());
  }

  public List<AnomalieDuPointage> anomalies() {
    return finAutomatique() ? List.of(AnomalieDuPointage.FIN_AUTOMATIQUE) : List.of();
  }

  public Montant machine() {
    return tranche.coutMachine();
  }

  /**
   * La somme de ses parts, deja au centime.
   */
  public Montant mainDOeuvre() {
    return parts.stream().map(TrancheValorisable::coutDeMainDOeuvre).reduce(Montant.ZERO, Montant::plus);
  }

  public boolean finAutomatique() {
    return tranche.finAutomatique();
  }

  /**
   * Un taux absent compte zero : la main d'oeuvre ou la machine a 0 EUR qui en resulte ne doit pas partir chez le
   * client. Un poste a 0 EUR de l'heure est valide, et un pointage sans poste n'a pas de machine a chiffrer.
   */
  public boolean tarifManquant() {
    Activite activite = activite();
    return activite.tauxHoraire().isEmpty() || (activite.poste().isPresent() && activite.coutHoraire().isEmpty());
  }
}

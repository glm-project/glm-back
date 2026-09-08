package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Une ligne du rapport : tout ce qui a ete fait sur l'element a une meme nature d'operation.
 *
 * <p>
 * C'est l'unite d'arrondi. Les tranches se somment a l'echelle de travail, la ligne arrondit une fois, et le rapport
 * totalise des lignes deja arrondies : sans quoi l'ecran afficherait un total qui ne serait pas la somme de ce qu'il
 * montre.
 * </p>
 */
public record LigneDeCout(Optional<NatureDOperation> nature, Periode periode, TempsPasse temps, List<Periode> nonConformites, Cout cout) {
  private static final Comparator<Periode> PAR_DEBUT = Comparator.comparing(Periode::debut).thenComparing(Periode::fin);

  public LigneDeCout {
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("periode", periode);
    Assert.notNull("temps", temps);
    Assert.field("non conformites", nonConformites).notNull().noNullElement();
    Assert.notNull("cout", cout);
  }

  static LigneDeCoutNatureBuilder builder() {
    return nature -> periode -> temps -> nonConformites -> cout -> new LigneDeCout(nature, periode, temps, nonConformites, cout);
  }

  /**
   * La ligne deduite des tranches d'une meme nature, chacune decoupee sur les sous-periodes ou le diviseur de son
   * operateur est constant.
   */
  static LigneDeCout de(Optional<NatureDOperation> nature, List<TrancheDActivite> tranches, ChargesDesOperateurs charges) {
    List<TrancheValorisable> parts = tranches
      .stream()
      .flatMap(tranche -> charges.decoupe(tranche).stream())
      .toList();

    return builder()
      .nature(nature)
      .periode(periode(tranches))
      .temps(temps(tranches))
      .nonConformites(nonConformites(tranches))
      .cout(cout(parts));
  }

  private static Periode periode(List<TrancheDActivite> tranches) {
    Instant debut = tranches
      .stream()
      .map(tranche -> tranche.periode().debut())
      .min(Comparator.naturalOrder())
      .orElseThrow();
    Instant fin = tranches
      .stream()
      .map(tranche -> tranche.periode().fin())
      .max(Comparator.naturalOrder())
      .orElseThrow();

    return new Periode(debut, fin);
  }

  private static TempsPasse temps(List<TrancheDActivite> tranches) {
    return new TempsPasse(duree(tranches, CategorieDActivite.TRAVAIL), duree(tranches, CategorieDActivite.NON_CONFORMITE));
  }

  private static Duration duree(List<TrancheDActivite> tranches, CategorieDActivite categorie) {
    return tranches
      .stream()
      .filter(tranche -> tranche.activite().categorie() == categorie)
      .map(TrancheDActivite::duree)
      .reduce(Duration.ZERO, Duration::plus);
  }

  /**
   * Les periodes de reprise, telles qu'elles ont ete pointees : ce sont les tranches qui les portent, jamais leur
   * decoupage par le parallelisme, qui n'a de sens que pour le calcul.
   */
  private static List<Periode> nonConformites(List<TrancheDActivite> tranches) {
    return tranches
      .stream()
      .filter(tranche -> tranche.activite().categorie() == CategorieDActivite.NON_CONFORMITE)
      .map(TrancheDActivite::periode)
      .sorted(PAR_DEBUT)
      .toList();
  }

  private static Cout cout(List<TrancheValorisable> parts) {
    return new Cout(
      new Montant(somme(parts, TrancheValorisable::coutMachine)),
      new Montant(somme(parts, TrancheValorisable::coutDeMainDOeuvre))
    );
  }

  private static BigDecimal somme(List<TrancheValorisable> parts, java.util.function.Function<TrancheValorisable, BigDecimal> montant) {
    return parts.stream().map(montant).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  interface LigneDeCoutNatureBuilder {
    LigneDeCoutPeriodeBuilder nature(Optional<NatureDOperation> nature);
  }

  interface LigneDeCoutPeriodeBuilder {
    LigneDeCoutTempsBuilder periode(Periode periode);
  }

  interface LigneDeCoutTempsBuilder {
    LigneDeCoutNonConformitesBuilder temps(TempsPasse temps);
  }

  interface LigneDeCoutNonConformitesBuilder {
    LigneDeCoutCoutBuilder nonConformites(List<Periode> nonConformites);
  }

  interface LigneDeCoutCoutBuilder {
    LigneDeCout cout(Cout cout);
  }
}

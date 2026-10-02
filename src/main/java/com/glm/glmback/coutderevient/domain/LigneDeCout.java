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
public record LigneDeCout(
  Optional<NatureDOperation> nature,
  Plage periode,
  TempsPasse temps,
  List<Periode> nonConformites,
  List<Periode> finsAutomatiques,
  Cout cout
) {
  private static final Comparator<Periode> PAR_DEBUT = Comparator.comparing(Periode::debut).thenComparing(Periode::fin);

  public LigneDeCout {
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("periode", periode);
    Assert.notNull("temps", temps);
    Assert.field("non conformites", nonConformites).notNull().noNullElement();
    Assert.field("fins automatiques", finsAutomatiques).notNull().noNullElement();
    Assert.notNull("cout", cout);
  }

  private LigneDeCout(Builder builder) {
    this(builder.nature, builder.periode, builder.temps, builder.nonConformites, builder.finsAutomatiques, builder.cout);
  }

  static LigneDeCoutNatureBuilder builder() {
    return new Builder();
  }

  private static final class Builder
    implements
      LigneDeCoutNatureBuilder,
      LigneDeCoutPeriodeBuilder,
      LigneDeCoutTempsBuilder,
      LigneDeCoutNonConformitesBuilder,
      LigneDeCoutFinsAutomatiquesBuilder,
      LigneDeCoutCoutBuilder
  {

    private Optional<NatureDOperation> nature;
    private Plage periode;
    private TempsPasse temps;
    private List<Periode> nonConformites;
    private List<Periode> finsAutomatiques;
    private Cout cout;

    @Override
    public LigneDeCoutPeriodeBuilder nature(Optional<NatureDOperation> nature) {
      this.nature = nature;
      return this;
    }

    @Override
    public LigneDeCoutTempsBuilder periode(Plage periode) {
      this.periode = periode;
      return this;
    }

    @Override
    public LigneDeCoutNonConformitesBuilder temps(TempsPasse temps) {
      this.temps = temps;
      return this;
    }

    @Override
    public LigneDeCoutFinsAutomatiquesBuilder nonConformites(List<Periode> nonConformites) {
      this.nonConformites = nonConformites;
      return this;
    }

    @Override
    public LigneDeCoutCoutBuilder finsAutomatiques(List<Periode> finsAutomatiques) {
      this.finsAutomatiques = finsAutomatiques;
      return this;
    }

    @Override
    public LigneDeCout cout(Cout cout) {
      this.cout = cout;
      return new LigneDeCout(this);
    }
  }

  /**
   * La ligne deduite des tranches d'une meme nature, chacune decoupee sur les sous-periodes ou le diviseur de son
   * operateur est constant.
   */
  static LigneDeCout de(TravailDeLaLigne travail, ChargesDesOperateurs charges) {
    List<TrancheDActivite> tranches = travail.terminees();
    List<TrancheValorisable> parts = tranches
      .stream()
      .flatMap(tranche -> charges.decoupe(tranche).stream())
      .toList();
    return builder()
      .nature(travail.nature())
      .periode(periode(travail))
      .temps(temps(travail))
      .nonConformites(nonConformites(tranches))
      .finsAutomatiques(
        tranches.stream().filter(TrancheDActivite::finAutomatique).map(TrancheDActivite::periode).sorted(PAR_DEBUT).toList()
      )
      .cout(travail.aResoudre().isEmpty() ? cout(tranches, parts) : new Cout(MontantTotal.incomplet(), MontantTotal.incomplet()));
  }

  private static Plage periode(TravailDeLaLigne travail) {
    Instant debut = java.util.stream.Stream.concat(
      travail
        .terminees()
        .stream()
        .map(tranche -> tranche.periode().debut()),
      travail
        .aResoudre()
        .stream()
        .map(activite -> activite.plage().debut())
    )
      .min(Comparator.naturalOrder())
      .orElseThrow();
    Optional<Instant> fin = travail
      .terminees()
      .stream()
      .map(tranche -> tranche.periode().fin())
      .max(Comparator.naturalOrder());
    return new Plage(debut, fin);
  }

  private static TempsPasse temps(TravailDeLaLigne travail) {
    return new TempsPasse(duree(travail, CategorieDActivite.TRAVAIL), duree(travail, CategorieDActivite.NON_CONFORMITE));
  }

  private static DureeTotale duree(TravailDeLaLigne travail, CategorieDActivite categorie) {
    return travail
        .aResoudre()
        .stream()
        .anyMatch(activite -> activite.activite().categorie() == categorie)
      ? DureeTotale.incomplet()
      : DureeTotale.de(duree(travail.terminees(), categorie));
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

  /**
   * La machine se somme tranche par tranche, chacune deja arrondie : elle n'est jamais partagee, donc jamais
   * decoupee pour etre chiffree.
   */
  private static Cout cout(List<TrancheDActivite> tranches, List<TrancheValorisable> parts) {
    return new Cout(
      MontantTotal.de(tranches.stream().map(TrancheDActivite::coutMachine).reduce(Montant.ZERO, Montant::plus)),
      parts.stream().allMatch(part -> part.coutDeMainDOeuvre().isPresent())
        ? MontantTotal.de(
            new Montant(
              parts
                .stream()
                .map(part -> part.coutDeMainDOeuvre().orElseThrow())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
            )
          )
        : MontantTotal.incomplet()
    );
  }

  interface LigneDeCoutNatureBuilder {
    LigneDeCoutPeriodeBuilder nature(Optional<NatureDOperation> nature);
  }

  interface LigneDeCoutPeriodeBuilder {
    LigneDeCoutTempsBuilder periode(Plage periode);
  }

  interface LigneDeCoutTempsBuilder {
    LigneDeCoutNonConformitesBuilder temps(TempsPasse temps);
  }

  interface LigneDeCoutNonConformitesBuilder {
    LigneDeCoutFinsAutomatiquesBuilder nonConformites(List<Periode> nonConformites);
  }

  interface LigneDeCoutFinsAutomatiquesBuilder {
    LigneDeCoutCoutBuilder finsAutomatiques(List<Periode> finsAutomatiques);
  }

  interface LigneDeCoutCoutBuilder {
    LigneDeCout cout(Cout cout);
  }
}

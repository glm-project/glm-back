package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Une ligne du rapport : tout ce qui a ete fait sur l'element a une meme nature d'operation.
 *
 * <p>
 * La ligne n'arrondit plus rien : elle additionne des montants deja au centime, la machine de chaque activite et la
 * main d'oeuvre de chaque part repartie dans sa fenetre de partage. Le rapport totalise des lignes, et chaque total
 * est ainsi exactement la somme de ce que l'ecran montre.
 * </p>
 */
public record LigneDeCout(
  Optional<NatureDOperation> nature,
  Plage periode,
  TempsPasse temps,
  List<Periode> nonConformites,
  List<Periode> finsAutomatiques,
  List<PointageDeCout> pointages,
  Cout cout
) {
  private static final Comparator<Periode> PAR_DEBUT = Comparator.comparing(Periode::debut).thenComparing(Periode::fin);

  public LigneDeCout {
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("periode", periode);
    Assert.notNull("temps", temps);
    Assert.field("non conformites", nonConformites).notNull().noNullElement();
    Assert.field("fins automatiques", finsAutomatiques).notNull().noNullElement();
    Assert.field("pointages", pointages).notNull().noNullElement();
    Assert.notNull("cout", cout);
    pointages = List.copyOf(pointages);
  }

  private LigneDeCout(Builder builder) {
    this(builder.nature, builder.periode, builder.temps, builder.nonConformites, builder.finsAutomatiques, builder.pointages, builder.cout);
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
      LigneDeCoutPointagesBuilder,
      LigneDeCoutCoutBuilder
  {

    private Optional<NatureDOperation> nature;
    private Plage periode;
    private TempsPasse temps;
    private List<Periode> nonConformites;
    private List<Periode> finsAutomatiques;
    private List<PointageDeCout> pointages;
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
    public LigneDeCoutPointagesBuilder finsAutomatiques(List<Periode> finsAutomatiques) {
      this.finsAutomatiques = finsAutomatiques;
      return this;
    }

    @Override
    public LigneDeCoutCoutBuilder pointages(List<PointageDeCout> pointages) {
      this.pointages = pointages;
      return this;
    }

    @Override
    public LigneDeCout cout(Cout cout) {
      this.cout = cout;
      return new LigneDeCout(this);
    }
  }

  /**
   * La ligne deduite des tranches d'une meme nature, chacune decoupee sur les fenetres de partage de son
   * operateur.
   */
  static LigneDeCout de(TravailDeLaLigne travail, ChargesDesOperateurs charges, List<SequenceEnConflit> conflits) {
    List<TrancheDActivite> tranches = travail.terminees();
    List<PointageValorise> valorises = tranches
      .stream()
      .map(tranche -> new PointageValorise(tranche, charges.decoupe(tranche)))
      .toList();
    List<TrancheValorisable> parts = valorises
      .stream()
      .flatMap(pointage -> pointage.parts().stream())
      .toList();
    return builder()
      .nature(travail.nature())
      .periode(periode(travail))
      .temps(temps(travail))
      .nonConformites(nonConformites(tranches))
      .finsAutomatiques(
        tranches.stream().filter(TrancheDActivite::finAutomatique).map(TrancheDActivite::periode).sorted(PAR_DEBUT).toList()
      )
      .pointages(pointages(valorises, travail.aResoudre(), conflits))
      .cout(travail.aResoudre().isEmpty() ? cout(tranches, parts) : new Cout(MontantTotal.incomplet(), MontantTotal.incomplet()));
  }

  /**
   * Les pointages de la ligne dans l'ordre ou ils ont commence, ceux a resoudre compris : le detail les montre tous,
   * sans quoi la somme qu'il justifie aurait des trous. L'ordre de lecture departage deux debuts identiques.
   */
  private static List<PointageDeCout> pointages(
    List<PointageValorise> valorises,
    List<ActiviteInterpretee> aResoudre,
    List<SequenceEnConflit> conflits
  ) {
    return java.util.stream.Stream.<PointageDeCout>concat(
      valorises.stream(),
      aResoudre.stream().map(activite -> new PointageAResoudre(activite, contradictoires(activite, conflits)))
    )
      .sorted(Comparator.comparing(PointageDeCout::debut))
      .toList();
  }

  private static List<PointageEnConflit> contradictoires(ActiviteInterpretee activite, List<SequenceEnConflit> conflits) {
    return conflits
      .stream()
      .filter(sequence -> sequence.activites().contains(activite.id()))
      .flatMap(sequence -> sequence.pointages().stream())
      .distinct()
      .toList();
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
            parts
              .stream()
              .map(part -> part.coutDeMainDOeuvre().orElseThrow())
              .reduce(Montant.ZERO, Montant::plus)
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
    LigneDeCoutPointagesBuilder finsAutomatiques(List<Periode> finsAutomatiques);
  }

  interface LigneDeCoutPointagesBuilder {
    LigneDeCoutCoutBuilder pointages(List<PointageDeCout> pointages);
  }

  interface LigneDeCoutCoutBuilder {
    LigneDeCout cout(Cout cout);
  }
}

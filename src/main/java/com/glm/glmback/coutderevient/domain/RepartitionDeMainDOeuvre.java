package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;

/**
 * Le cout d'un operateur sur une fenetre de partage, arrondi une seule fois puis reparti en centimes entiers.
 *
 * <p>
 * Chaque part vaut taux horaire x duree / nombre de postes. Le total de la fenetre s'arrondit au centime, chaque part
 * recoit d'abord son montant arrondi au centime inferieur, et les centimes qui manquent vont aux plus forts restes,
 * puis a l'activite commencee la premiere, puis a un ordre stable des valeurs de la part. Une heure partagee vaut
 * ainsi exactement son cout, quel que soit l'element lu (ADR 0004).
 * </p>
 *
 * <p>
 * Deux parts identiques — meme poste, meme periode, memes tarifs — n'en font qu'une et recoivent le meme montant :
 * plusieurs activites sur un meme poste comptent un poste, et chaque element paie sa part entiere. Une part sans taux
 * horaire n'entre pas dans la repartition : elle ne coute rien.
 * </p>
 */
public record RepartitionDeMainDOeuvre(Map<TrancheDActivite, Montant> parts) {
  public static final RepartitionDeMainDOeuvre AUCUNE = new RepartitionDeMainDOeuvre(Map.of());

  private static final BigDecimal MILLISECONDES_PAR_HEURE = new BigDecimal(3_600_000);
  private static final int ECHELLE_DE_TRAVAIL = 10;
  private static final int CENTIMES = 2;
  private static final BigDecimal CENTIME = new BigDecimal("0.01");

  private static final Comparator<PartExacte> PAR_PRIORITE = Comparator.comparing(PartExacte::reste)
    .reversed()
    .thenComparing(PartExacte::debutDeLActivite)
    .thenComparing(part -> part.part().activite().poste().map(PosteDeTravailId::uuid).orElse(null), Comparator.nullsLast(UUID::compareTo))
    .thenComparing(part -> part.part().periode().debut())
    .thenComparing(part -> part.part().periode().fin())
    .thenComparing(part -> part.part().activite().categorie())
    .thenComparing(
      part -> part.part().activite().nature().map(NatureDOperation::value).orElse(null),
      Comparator.nullsLast(String::compareTo)
    )
    .thenComparing(part -> part.part().activite().tauxHoraire().orElseThrow().value())
    .thenComparing(
      part -> part.part().activite().coutHoraire().map(CoutHoraire::value).orElse(null),
      Comparator.nullsLast(BigDecimal::compareTo)
    )
    .thenComparing(part -> part.part().finAutomatique());

  public RepartitionDeMainDOeuvre {
    Assert.notNull("parts", parts);
  }

  /**
   * La repartition des tranches de l'operateur sur la fenetre donnee, ou le diviseur est constant.
   */
  public static RepartitionDeMainDOeuvre de(List<TrancheDActivite> tranches, Periode fenetre, Diviseur diviseur) {
    Map<TrancheDActivite, Instant> debuts = new HashMap<>();
    tranches
      .stream()
      .filter(tranche -> tranche.activite().tauxHoraire().isPresent())
      .forEach(tranche ->
        tranche
          .reduiteA(fenetre)
          .ifPresent(part -> debuts.merge(part, tranche.periode().debut(), BinaryOperator.minBy(Comparator.naturalOrder())))
      );
    List<PartExacte> exactes = debuts
      .entrySet()
      .stream()
      .map(entree -> PartExacte.de(entree.getKey(), entree.getValue(), diviseur))
      .sorted(PAR_PRIORITE)
      .toList();

    return new RepartitionDeMainDOeuvre(Map.copyOf(repartir(exactes)));
  }

  private static Map<TrancheDActivite, Montant> repartir(List<PartExacte> exactes) {
    BigDecimal total = exactes
      .stream()
      .map(PartExacte::exact)
      .reduce(BigDecimal.ZERO, BigDecimal::add)
      .setScale(CENTIMES, RoundingMode.HALF_UP);
    BigDecimal planchers = exactes.stream().map(PartExacte::plancher).reduce(BigDecimal.ZERO, BigDecimal::add);
    int centimesRestants = total.subtract(planchers).movePointRight(CENTIMES).intValueExact();

    Map<TrancheDActivite, Montant> montants = new HashMap<>();
    for (int rang = 0; rang < exactes.size(); rang++) {
      PartExacte part = exactes.get(rang);
      BigDecimal montant = rang < centimesRestants ? part.plancher().add(CENTIME) : part.plancher();
      montants.put(part.part(), new Montant(montant));
    }
    return montants;
  }

  /**
   * Ce que la part donnee recoit, si elle appartient a la fenetre et porte un taux horaire.
   */
  public Optional<Montant> de(TrancheDActivite part) {
    return Optional.ofNullable(parts.get(part));
  }

  private record PartExacte(TrancheDActivite part, Instant debutDeLActivite, BigDecimal exact) {
    private static PartExacte de(TrancheDActivite part, Instant debutDeLActivite, Diviseur diviseur) {
      return new PartExacte(
        part,
        debutDeLActivite,
        part
          .activite()
          .tauxHoraire()
          .orElseThrow()
          .value()
          .multiply(new BigDecimal(part.duree().toMillis()))
          .divide(MILLISECONDES_PAR_HEURE.multiply(new BigDecimal(diviseur.value())), ECHELLE_DE_TRAVAIL, RoundingMode.HALF_UP)
      );
    }

    private BigDecimal plancher() {
      return exact.setScale(CENTIMES, RoundingMode.FLOOR);
    }

    private BigDecimal reste() {
      return exact.subtract(plancher());
    }
  }
}

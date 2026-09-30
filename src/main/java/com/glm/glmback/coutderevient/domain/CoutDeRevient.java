package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Le rapport d'un element de fabrication : une ligne par nature d'operation, et le total.
 *
 * <p>
 * Aucune identite, aucune persistance : l'objet nait et meurt dans l'appel, recalcule depuis les activites interpretees
 * par atelier. Une saisie regularisee apres coup compte donc a l'heure ou le travail a eu lieu.
 * </p>
 */
public record CoutDeRevient(ElementValorise element, List<LigneDeCout> lignes, EvaluationDuCout lecture, List<SequenceEnConflit> conflits) {
  /**
   * Les natures dans l'ordre alphabetique, et la ligne sans nature en dernier : elle est le residu de ce qui a ete
   * pointe sans poste, et n'a pas de place dans l'ordre des metiers.
   */
  private static final Comparator<Optional<NatureDOperation>> PAR_NATURE = Comparator.comparing(
    (Optional<NatureDOperation> nature) -> nature.map(NatureDOperation::value).orElse(null),
    Comparator.nullsLast(Comparator.naturalOrder())
  );

  public CoutDeRevient {
    Assert.notNull("element", element);
    Assert.field("lignes", lignes).notNull().noNullElement();
    Assert.notNull("lecture", lecture);
    Assert.field("conflits", conflits).notNull().noNullElement();
  }

  public static ElementBuilder builder() {
    return element ->
      tranches ->
        aResoudre -> charges -> lecture -> conflits -> new CoutDeRevient(element, lignes(tranches, charges, aResoudre), lecture, conflits);
  }

  public interface ElementBuilder {
    TranchesBuilder element(ElementValorise element);
  }

  public interface TranchesBuilder {
    AResoudreBuilder tranches(List<TrancheDActivite> tranches);
  }

  public interface AResoudreBuilder {
    ChargesBuilder aResoudre(List<ActiviteInterpretee> aResoudre);
  }

  public interface ChargesBuilder {
    LectureBuilder charges(ChargesDesOperateurs charges);
  }

  public interface LectureBuilder {
    ConflitsBuilder lecture(EvaluationDuCout lecture);
  }

  public interface ConflitsBuilder {
    CoutDeRevient conflits(List<SequenceEnConflit> conflits);
  }

  public TempsPasse temps() {
    return lignes.stream().map(LigneDeCout::temps).reduce(TempsPasse.AUCUN, TempsPasse::plus);
  }

  public Cout cout() {
    return lignes.stream().map(LigneDeCout::cout).reduce(Cout.AUCUN, Cout::plus);
  }

  private static List<LigneDeCout> lignes(
    List<TrancheDActivite> tranches,
    ChargesDesOperateurs charges,
    List<ActiviteInterpretee> aResoudre
  ) {
    List<Optional<NatureDOperation>> natures = java.util.stream.Stream.concat(
      tranches.stream().map(tranche -> tranche.activite().nature()),
      aResoudre.stream().map(activite -> activite.activite().nature())
    )
      .distinct()
      .sorted(PAR_NATURE)
      .toList();
    return natures
      .stream()
      .map(nature ->
        LigneDeCout.de(
          new TravailDeLaLigne(
            nature,
            tranches
              .stream()
              .filter(tranche -> tranche.activite().nature().equals(nature))
              .toList(),
            aResoudre
              .stream()
              .filter(activite -> activite.activite().nature().equals(nature))
              .toList()
          ),
          charges
        )
      )
      .toList();
  }
}

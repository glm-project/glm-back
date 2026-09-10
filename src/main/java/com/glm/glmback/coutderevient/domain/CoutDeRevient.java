package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Le rapport d'un element de fabrication : une ligne par nature d'operation, et le total.
 *
 * <p>
 * Aucune identite, aucune persistance : l'objet nait et meurt dans l'appel, recalcule depuis les journaux de
 * l'atelier. Une saisie regularisee apres coup compte donc a l'heure ou le travail a eu lieu.
 * </p>
 */
public record CoutDeRevient(ElementValorise element, List<LigneDeCout> lignes) {
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
  }

  public static CoutDeRevient de(ElementValorise element, List<TrancheDActivite> tranches, ChargesDesOperateurs charges) {
    return new CoutDeRevient(element, lignes(tranches, charges));
  }

  public TempsPasse temps() {
    return lignes.stream().map(LigneDeCout::temps).reduce(TempsPasse.AUCUN, TempsPasse::plus);
  }

  public Cout cout() {
    return lignes.stream().map(LigneDeCout::cout).reduce(Cout.AUCUN, Cout::plus);
  }

  private static List<LigneDeCout> lignes(List<TrancheDActivite> tranches, ChargesDesOperateurs charges) {
    Map<Optional<NatureDOperation>, List<TrancheDActivite>> parNature = tranches
      .stream()
      .collect(Collectors.groupingBy(tranche -> tranche.activite().nature(), LinkedHashMap::new, Collectors.toList()));

    return parNature
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey(PAR_NATURE))
      .map(entree -> LigneDeCout.de(entree.getKey(), entree.getValue(), charges))
      .toList();
  }
}

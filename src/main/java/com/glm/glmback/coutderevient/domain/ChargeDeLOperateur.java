package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Ce qu'un operateur menait de front, decoupe en sous-periodes ou le nombre de postes occupes ne change pas.
 *
 * <p>
 * Le diviseur compte des <strong>postes</strong>, jamais des elements ni des activites : le client enonce la regle
 * deux fois de suite, cout horaire de chaque machine active non divise, taux horaire de l'operateur divise par le
 * nombre de machines qu'il utilise. Un operateur sur trois elements avec une seule machine n'est donc pas divise.
 * </p>
 *
 * <p>
 * Un pointage sans poste compte pour un poste, comme la cle d'activite de l'atelier ou l'absence de poste est une
 * valeur : une entreprise sans parc machine retrouve un diviseur de un partout.
 * </p>
 */
public record ChargeDeLOperateur(List<SousPeriode> sousPeriodes) {
  public ChargeDeLOperateur {
    Assert.field("sous periodes", sousPeriodes).notNull().noNullElement();
  }

  /**
   * La charge deduite de tout ce que l'operateur a fait, tous elements confondus.
   *
   * <p>
   * Toutes elements confondus, parce que c'est la question posee : un nouveau pointage sur un second element change
   * la part deja attribuee au premier. Le reparti traverse les agregats, il ne peut donc etre qu'une projection.
   * </p>
   */
  static ChargeDeLOperateur de(List<TrancheDActivite> tranches) {
    List<Instant> bornes = bornes(tranches);
    List<SousPeriode> sousPeriodes = new ArrayList<>();

    for (int rang = 0; rang + 1 < bornes.size(); rang++) {
      Periode candidate = new Periode(bornes.get(rang), bornes.get(rang + 1));
      sousPeriode(tranches, candidate).ifPresent(sousPeriodes::add);
    }

    return new ChargeDeLOperateur(List.copyOf(sousPeriodes));
  }

  /**
   * La tranche donnee, decoupee en autant de parts que de sous-periodes qu'elle traverse.
   */
  public List<TrancheValorisable> decoupe(TrancheDActivite tranche) {
    return sousPeriodes
      .stream()
      .flatMap(sousPeriode ->
        tranche
          .reduiteA(sousPeriode.periode())
          .map(part -> new TrancheValorisable(part, sousPeriode.diviseur()))
          .stream()
      )
      .toList();
  }

  private static List<Instant> bornes(List<TrancheDActivite> tranches) {
    TreeSet<Instant> bornes = new TreeSet<>();
    tranches.forEach(tranche -> {
      bornes.add(tranche.periode().debut());
      bornes.add(tranche.periode().fin());
    });

    return List.copyOf(bornes);
  }

  /**
   * Rien n'est rendu la ou l'operateur ne travaillait pas : entre deux pointages, il n'y a aucun diviseur a poser.
   */
  private static Optional<SousPeriode> sousPeriode(List<TrancheDActivite> tranches, Periode candidate) {
    long postes = tranches
      .stream()
      .filter(tranche -> tranche.periode().intersection(candidate).isPresent())
      .map(tranche -> tranche.activite().poste())
      .distinct()
      .count();

    if (postes == 0) {
      return Optional.empty();
    }

    return Optional.of(new SousPeriode(candidate, new Diviseur(Math.toIntExact(postes))));
  }
}

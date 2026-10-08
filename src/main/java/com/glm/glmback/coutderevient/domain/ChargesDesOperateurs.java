package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * La charge de chaque operateur, seul juge du diviseur qui s'applique a ses tranches.
 *
 * <p>
 * Le parallelisme se compte par personne : deux operateurs qui travaillent en meme temps ne se divisent pas l'un
 * l'autre, chacun etant paye son heure entiere.
 * </p>
 */
public record ChargesDesOperateurs(Map<OperateurId, ChargeDeLOperateur> parOperateur) {
  public ChargesDesOperateurs {
    Assert.notNull("charges par operateur", parOperateur);
  }

  public static ChargesDesOperateurs de(List<TrancheDActivite> tranches) {
    return new ChargesDesOperateurs(
      tranches
        .stream()
        .collect(
          Collectors.groupingBy(TrancheDActivite::operateur, Collectors.collectingAndThen(Collectors.toList(), ChargeDeLOperateur::de))
        )
    );
  }

  /**
   * La tranche donnee, decoupee sur les sous-periodes de son propre operateur.
   *
   * <p>
   * Une tranche dont l'operateur est inconnu ne rend rien : le service construit ces charges sur l'union de ce qu'il
   * valorise et de ce qu'il a lu, donc le cas ne se produit pas en production. Le rendre vide plutot que de le
   * supposer evite d'avoir a le supposer.
   * </p>
   */
  public List<TrancheValorisable> decoupe(TrancheDActivite tranche) {
    return Optional.ofNullable(parOperateur.get(tranche.operateur()))
      .map(charge -> charge.decoupe(tranche))
      .orElseGet(List::of);
  }
}

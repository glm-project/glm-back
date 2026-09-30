package com.glm.glmback.atelier.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resout les identifiants d'un journal en ressources affichables.
 *
 * <p>
 * Une lecture, deux requetes au plus, quel que soit le nombre d'evenements : c'est la raison d'etre des acces par
 * ensemble des deux ports.
 * </p>
 */
public final class AnnuaireDAtelierService {

  private final OperateursConnus operateurs;
  private final PostesConnus postes;

  public AnnuaireDAtelierService(OperateursConnus operateurs, PostesConnus postes) {
    this.operateurs = operateurs;
    this.postes = postes;
  }

  public AnnuaireDAtelier pour(SuiviDAtelier suivi) {
    return pourSuivis(List.of(suivi));
  }

  public AnnuaireDAtelier pourSuivis(Collection<SuiviDAtelier> suivis) {
    List<EvenementDAtelier> evenements = suivis
      .stream()
      .flatMap(suivi -> suivi.journal().evenements().stream())
      .toList();

    return resout(
      evenements.stream().map(EvenementDAtelier::operateur).collect(Collectors.toSet()),
      evenements
        .stream()
        .flatMap(evenement -> evenement.poste().stream())
        .collect(Collectors.toSet())
    );
  }

  public AnnuaireDAtelier pourIntervalles(Collection<IntervalleDActivite> intervalles) {
    return resout(
      intervalles.stream().map(IntervalleDActivite::operateur).collect(Collectors.toSet()),
      intervalles
        .stream()
        .flatMap(intervalle -> intervalle.poste().stream())
        .collect(Collectors.toSet())
    );
  }

  private AnnuaireDAtelier resout(Set<OperateurId> operateursAResoudre, Set<PosteDeTravailId> postesAResoudre) {
    return AnnuaireDAtelier.de(operateurs.parIds(operateursAResoudre), postes.parIds(postesAResoudre));
  }
}

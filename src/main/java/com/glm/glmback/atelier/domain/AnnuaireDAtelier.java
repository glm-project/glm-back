package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Les ressources d'une lecture, resolues une fois pour toutes.
 *
 * <p>
 * Ce n'est pas un agregat : c'est la lecture composee que les ecrans consomment, sur le modele du profil d'un
 * operateur. Le journal ne stockant que des identifiants, un affichage doit les resoudre — et le faire ici, en un seul
 * aller par port, plutot qu'une requete par evenement.
 * </p>
 *
 * <p>
 * Une ressource absente rend un {@link Optional} vide plutot qu'une erreur : le referentiel refuse de supprimer ce qui
 * a servi a pointer, mais l'affichage d'un journal n'est pas l'endroit ou faire echouer une lecture.
 * </p>
 */
public record AnnuaireDAtelier(Map<OperateurId, OperateurConnu> operateurs, Map<PosteDeTravailId, PosteConnu> postes) {
  public AnnuaireDAtelier {
    Assert.notNull("operateurs", operateurs);
    Assert.notNull("postes", postes);

    operateurs = Map.copyOf(operateurs);
    postes = Map.copyOf(postes);
  }

  static AnnuaireDAtelier de(List<OperateurConnu> operateurs, List<PosteConnu> postes) {
    return new AnnuaireDAtelier(
      operateurs.stream().collect(Collectors.toMap(OperateurConnu::id, Function.identity())),
      postes.stream().collect(Collectors.toMap(PosteConnu::id, Function.identity()))
    );
  }

  public Optional<OperateurConnu> operateur(OperateurId id) {
    return Optional.ofNullable(operateurs.get(id));
  }

  public Optional<PosteConnu> poste(PosteDeTravailId id) {
    return Optional.ofNullable(postes.get(id));
  }
}

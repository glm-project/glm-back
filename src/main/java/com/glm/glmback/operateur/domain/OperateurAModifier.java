package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public record OperateurAModifier(
  OperateurId id,
  Nom nom,
  Prenom prenom,
  Optional<Matricule> matricule,
  Optional<TauxHoraire> tauxHoraire,
  Set<PosteHabilitableId> postes
) {
  public OperateurAModifier {
    Assert.notNull("id", id);
    Assert.notNull("nom", nom);
    Assert.notNull("prenom", prenom);
    Assert.notNull("matricule", matricule);
    Assert.notNull("taux horaire", tauxHoraire);
    Assert.field("postes", postes).notNull().noNullElement();

    postes = Set.copyOf(postes);
  }

  public OperateurAModifier(
    OperateurId id,
    String nom,
    String prenom,
    String matricule,
    BigDecimal tauxHoraire,
    Set<PosteHabilitableId> postes
  ) {
    this(id, new Nom(nom), new Prenom(prenom), Matricule.of(matricule), TauxHoraire.of(tauxHoraire), postes);
  }
}

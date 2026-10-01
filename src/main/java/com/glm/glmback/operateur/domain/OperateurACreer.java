package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public record OperateurACreer(
  Nom nom,
  Prenom prenom,
  Optional<Identifiant> identifiant,
  Optional<TauxHoraire> tauxHoraire,
  Set<PosteHabilitableId> postes
) {
  public OperateurACreer {
    Assert.notNull("nom", nom);
    Assert.notNull("prenom", prenom);
    Assert.notNull("identifiant", identifiant);
    Assert.notNull("taux horaire", tauxHoraire);
    Assert.field("postes", postes).notNull().noNullElement();

    postes = Set.copyOf(postes);
  }

  public OperateurACreer(String nom, String prenom, String identifiant, BigDecimal tauxHoraire, Set<PosteHabilitableId> postes) {
    this(new Nom(nom), new Prenom(prenom), Identifiant.of(identifiant), TauxHoraire.of(tauxHoraire), postes);
  }
}

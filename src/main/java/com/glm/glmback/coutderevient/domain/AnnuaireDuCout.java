package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/**
 * Les noms de tout ce que le detail d'un rapport cite : operateurs, postes et elements, y compris ceux d'un autre
 * element qui partageaient le temps d'un operateur.
 *
 * <p>
 * Rien n'est copie : chaque nom est relu au referentiel a chaque lecture. Un nom absent ne fait pas echouer le
 * rapport : le detail garde alors l'identifiant seul.
 * </p>
 */
public record AnnuaireDuCout(List<OperateurNomme> operateurs, List<PosteNomme> postes, List<ElementValorise> elements) {
  public static final AnnuaireDuCout VIDE = new AnnuaireDuCout(List.of(), List.of(), List.of());

  public AnnuaireDuCout {
    Assert.field("operateurs", operateurs).notNull().noNullElement();
    Assert.field("postes", postes).notNull().noNullElement();
    Assert.field("elements", elements).notNull().noNullElement();
    operateurs = List.copyOf(operateurs);
    postes = List.copyOf(postes);
    elements = List.copyOf(elements);
  }

  public Optional<OperateurNomme> operateur(OperateurId operateur) {
    return operateurs
      .stream()
      .filter(nomme -> nomme.operateur().equals(operateur))
      .findFirst();
  }

  public Optional<PosteNomme> poste(PosteDeTravailId poste) {
    return postes
      .stream()
      .filter(nomme -> nomme.poste().equals(poste))
      .findFirst();
  }

  public Optional<ElementValorise> element(ElementId element) {
    return elements
      .stream()
      .filter(valorise -> valorise.element().equals(element))
      .findFirst();
  }
}

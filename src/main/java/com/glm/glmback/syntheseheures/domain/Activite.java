package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce que l'operateur faisait : sur quel element, a quel poste, a quel metier, et si c'etait du bon travail ou sa
 * reprise.
 *
 * <p>
 * L'activite dit tout sauf quand : c'est ce qui permet de la couper sur les fenetres de presence puis a minuit sans
 * jamais recopier ses composants.
 * </p>
 */
public record Activite(
  ElementId element,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  CategorieDActivite categorie
) {
  public Activite {
    Assert.notNull("element", element);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("categorie", categorie);
  }

  static ActiviteElementBuilder builder() {
    return element -> poste -> nature -> categorie -> new Activite(element, poste, nature, categorie);
  }

  interface ActiviteElementBuilder {
    ActivitePosteBuilder element(ElementId element);
  }

  interface ActivitePosteBuilder {
    ActiviteNatureBuilder poste(Optional<PosteDeTravailId> poste);
  }

  interface ActiviteNatureBuilder {
    ActiviteCategorieBuilder nature(Optional<NatureDOperation> nature);
  }

  interface ActiviteCategorieBuilder {
    Activite categorie(CategorieDActivite categorie);
  }
}

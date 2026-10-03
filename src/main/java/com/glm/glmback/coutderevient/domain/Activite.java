package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce qui se passait pendant une tranche de temps : qui, sur quoi, a quel metier, a quels tarifs, et si c'etait du bon
 * travail ou sa reprise.
 *
 * <p>
 * L'activite dit tout sauf quand : c'est ce qui permet de decouper une meme activite en autant de tranches que la
 * variations du parallelisme l'exigent, sans jamais recopier ses sept composants. L'element est ce sur quoi
 * l'operateur travaillait : il dit, pour un partage, ou etait l'autre moitie de son temps.
 * </p>
 */
public record Activite(
  OperateurId operateur,
  ElementId element,
  Optional<PosteDeTravailId> poste,
  Optional<NatureDOperation> nature,
  Optional<CoutHoraire> coutHoraire,
  Optional<TauxHoraire> tauxHoraire,
  CategorieDActivite categorie
) {
  public Activite {
    Assert.notNull("operateur", operateur);
    Assert.notNull("element", element);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("cout horaire", coutHoraire);
    Assert.notNull("taux horaire", tauxHoraire);
    Assert.notNull("categorie", categorie);
  }

  static ActiviteOperateurBuilder builder() {
    return operateur ->
      element ->
        poste ->
          nature ->
            coutHoraire -> tauxHoraire -> categorie -> new Activite(operateur, element, poste, nature, coutHoraire, tauxHoraire, categorie);
  }

  interface ActiviteOperateurBuilder {
    ActiviteElementBuilder operateur(OperateurId operateur);
  }

  interface ActiviteElementBuilder {
    ActivitePosteBuilder element(ElementId element);
  }

  interface ActivitePosteBuilder {
    ActiviteNatureBuilder poste(Optional<PosteDeTravailId> poste);
  }

  interface ActiviteNatureBuilder {
    ActiviteCoutHoraireBuilder nature(Optional<NatureDOperation> nature);
  }

  interface ActiviteCoutHoraireBuilder {
    ActiviteTauxHoraireBuilder coutHoraire(Optional<CoutHoraire> coutHoraire);
  }

  interface ActiviteTauxHoraireBuilder {
    ActiviteCategorieBuilder tauxHoraire(Optional<TauxHoraire> tauxHoraire);
  }

  interface ActiviteCategorieBuilder {
    Activite categorie(CategorieDActivite categorie);
  }
}

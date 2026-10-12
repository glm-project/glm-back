package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.util.Optional;

/**
 * Un pointage du rapport d'activite : la part d'une activite tombee dans un jour de la periode.
 *
 * <p>
 * Une activite qui passe minuit donne une ligne par jour. Une activite en cours n'a ni fin ni duree ; une fin
 * automatique compte jusqu'a son echeance, comme dans la synthese. Une ligne est simultanee quand une autre activite
 * terminee de l'operateur la recouvre : c'est ce qui explique qu'une journee compte plus que son amplitude.
 * </p>
 */
public record LigneDuJournal(
  ActiviteId activite,
  Plage plage,
  ElementEngage element,
  Optional<PosteConnu> poste,
  Optional<NatureDOperation> nature,
  CategorieDActivite categorie,
  EtatDActivite etat,
  boolean simultanee
) {
  public LigneDuJournal {
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
    Assert.notNull("element", element);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("categorie", categorie);
    Assert.notNull("etat de l'activite", etat);
  }

  static LigneActiviteBuilder builder() {
    return activite ->
      plage ->
        element ->
          poste ->
            nature ->
              categorie -> etat -> simultanee -> new LigneDuJournal(activite, plage, element, poste, nature, categorie, etat, simultanee);
  }

  public DureeTotale duree() {
    return DureeTotale.de(
      plage
        .fin()
        .map(fin -> Duration.between(plage.debut(), fin))
        .orElse(Duration.ZERO)
    );
  }

  interface LigneActiviteBuilder {
    LignePlageBuilder activite(ActiviteId activite);
  }

  interface LignePlageBuilder {
    LigneElementBuilder plage(Plage plage);
  }

  interface LigneElementBuilder {
    LignePosteBuilder element(ElementEngage element);
  }

  interface LignePosteBuilder {
    LigneNatureBuilder poste(Optional<PosteConnu> poste);
  }

  interface LigneNatureBuilder {
    LigneCategorieBuilder nature(Optional<NatureDOperation> nature);
  }

  interface LigneCategorieBuilder {
    LigneEtatBuilder categorie(CategorieDActivite categorie);
  }

  interface LigneEtatBuilder {
    LigneSimultaneeBuilder etat(EtatDActivite etat);
  }

  interface LigneSimultaneeBuilder {
    LigneDuJournal simultanee(boolean simultanee);
  }
}

package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le changement de poste que le gestionnaire demande pour une activite terminee : l'identifiant de la saisie, que le
 * client fournit une fois par saisie, l'activite visee, le nouveau poste et l'auteur. L'operateur, la categorie et les
 * heures sont ceux de l'activite.
 */
public record ChangementDePosteAEnregistrer(
  SuiviDAtelierId suivi,
  EvenementDAtelierId evenement,
  ActiviteId activite,
  PosteDeTravailId poste,
  Auteur auteur
) {
  public ChangementDePosteAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("id de l'evenement", evenement);
    Assert.notNull("activite", activite);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("auteur", auteur);
  }

  public static ChangementDePosteAEnregistrerSuiviBuilder builder() {
    return suivi ->
      evenement -> activite -> poste -> auteur -> new ChangementDePosteAEnregistrer(suivi, evenement, activite, poste, auteur);
  }

  public interface ChangementDePosteAEnregistrerSuiviBuilder {
    ChangementDePosteAEnregistrerEvenementBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface ChangementDePosteAEnregistrerEvenementBuilder {
    ChangementDePosteAEnregistrerActiviteBuilder evenement(EvenementDAtelierId evenement);
  }

  public interface ChangementDePosteAEnregistrerActiviteBuilder {
    ChangementDePosteAEnregistrerPosteBuilder activite(ActiviteId activite);
  }

  public interface ChangementDePosteAEnregistrerPosteBuilder {
    ChangementDePosteAEnregistrerAuteurBuilder poste(PosteDeTravailId poste);
  }

  public interface ChangementDePosteAEnregistrerAuteurBuilder {
    ChangementDePosteAEnregistrer auteur(Auteur auteur);
  }
}

package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le retrait d'une activite terminee : l'identifiant de la saisie, que le client fournit une fois par saisie, l'activite
 * visee et l'auteur. Rien n'est supprime du journal : le retrait y est un fait de plus.
 */
public record RetraitAEnregistrer(SuiviDAtelierId suivi, EvenementDAtelierId evenement, ActiviteId activite, Auteur auteur) {
  public RetraitAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("id de l'evenement", evenement);
    Assert.notNull("activite", activite);
    Assert.notNull("auteur", auteur);
  }

  public static RetraitAEnregistrerSuiviBuilder builder() {
    return suivi -> evenement -> activite -> auteur -> new RetraitAEnregistrer(suivi, evenement, activite, auteur);
  }

  public interface RetraitAEnregistrerSuiviBuilder {
    RetraitAEnregistrerEvenementBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface RetraitAEnregistrerEvenementBuilder {
    RetraitAEnregistrerActiviteBuilder evenement(EvenementDAtelierId evenement);
  }

  public interface RetraitAEnregistrerActiviteBuilder {
    RetraitAEnregistrerAuteurBuilder activite(ActiviteId activite);
  }

  public interface RetraitAEnregistrerAuteurBuilder {
    RetraitAEnregistrer auteur(Auteur auteur);
  }
}

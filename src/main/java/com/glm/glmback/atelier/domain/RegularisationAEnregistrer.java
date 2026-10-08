package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * La fin que le gestionnaire regularise sur une activite echue : l'identifiant de la saisie, que le client fournit et
 * garde d'un renvoi a l'autre, l'activite concernee, l'auteur qui la saisit et l'heure a laquelle la fin a eu lieu.
 *
 * <p>
 * L'operateur, le poste et le type du fait ne sont pas des donnees de la saisie : l'activite les porte, et la fin est
 * toujours une fin.
 * </p>
 */
public record RegularisationAEnregistrer(
  SuiviDAtelierId suivi,
  EvenementDAtelierId evenement,
  ActiviteId activite,
  Auteur auteur,
  Instant dateDeSurvenue
) {
  public RegularisationAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("evenement", evenement);
    Assert.notNull("activite", activite);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }

  public static RegularisationAEnregistrerSuiviBuilder builder() {
    return suivi ->
      evenement ->
        activite -> auteur -> dateDeSurvenue -> new RegularisationAEnregistrer(suivi, evenement, activite, auteur, dateDeSurvenue);
  }

  public interface RegularisationAEnregistrerSuiviBuilder {
    RegularisationAEnregistrerEvenementBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface RegularisationAEnregistrerEvenementBuilder {
    RegularisationAEnregistrerActiviteBuilder evenement(EvenementDAtelierId evenement);
  }

  public interface RegularisationAEnregistrerActiviteBuilder {
    RegularisationAEnregistrerAuteurBuilder activite(ActiviteId activite);
  }

  public interface RegularisationAEnregistrerAuteurBuilder {
    RegularisationAEnregistrerDateDeSurvenueBuilder auteur(Auteur auteur);
  }

  public interface RegularisationAEnregistrerDateDeSurvenueBuilder {
    RegularisationAEnregistrer dateDeSurvenue(Instant dateDeSurvenue);
  }
}

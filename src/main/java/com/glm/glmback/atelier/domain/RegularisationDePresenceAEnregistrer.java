package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Une presence rattrapee par le gestionnaire, sur une journee deja ouverte, a l'heure ou elle a reellement eu lieu.
 */
public record RegularisationDePresenceAEnregistrer(
  JourneeDeTravailId journee,
  TypeDEvenementDePresence type,
  Auteur auteur,
  Instant dateDeSurvenue
) {
  public RegularisationDePresenceAEnregistrer {
    Assert.notNull("journee", journee);
    Assert.notNull("type", type);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }

  public static RegularisationDePresenceAEnregistrerJourneeBuilder builder() {
    return journee -> type -> auteur -> dateDeSurvenue -> new RegularisationDePresenceAEnregistrer(journee, type, auteur, dateDeSurvenue);
  }

  public interface RegularisationDePresenceAEnregistrerJourneeBuilder {
    RegularisationDePresenceAEnregistrerTypeBuilder journee(JourneeDeTravailId journee);
  }

  public interface RegularisationDePresenceAEnregistrerTypeBuilder {
    RegularisationDePresenceAEnregistrerAuteurBuilder type(TypeDEvenementDePresence type);
  }

  public interface RegularisationDePresenceAEnregistrerAuteurBuilder {
    RegularisationDePresenceAEnregistrerDateDeSurvenueBuilder auteur(Auteur auteur);
  }

  public interface RegularisationDePresenceAEnregistrerDateDeSurvenueBuilder {
    RegularisationDePresenceAEnregistrer dateDeSurvenue(Instant dateDeSurvenue);
  }
}

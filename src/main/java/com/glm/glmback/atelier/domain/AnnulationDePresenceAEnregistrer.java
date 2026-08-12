package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record AnnulationDePresenceAEnregistrer(
  JourneeDeTravailId journee,
  EvenementDePresenceId evenement,
  Auteur auteur,
  MotifDAnnulation motif
) {
  public AnnulationDePresenceAEnregistrer {
    Assert.notNull("journee", journee);
    Assert.notNull("evenement", evenement);
    Assert.notNull("auteur", auteur);
    Assert.notNull("motif", motif);
  }

  public static AnnulationDePresenceAEnregistrerJourneeBuilder builder() {
    return journee -> evenement -> auteur -> motif -> new AnnulationDePresenceAEnregistrer(journee, evenement, auteur, motif);
  }

  public interface AnnulationDePresenceAEnregistrerJourneeBuilder {
    AnnulationDePresenceAEnregistrerEvenementBuilder journee(JourneeDeTravailId journee);
  }

  public interface AnnulationDePresenceAEnregistrerEvenementBuilder {
    AnnulationDePresenceAEnregistrerAuteurBuilder evenement(EvenementDePresenceId evenement);
  }

  public interface AnnulationDePresenceAEnregistrerAuteurBuilder {
    AnnulationDePresenceAEnregistrerMotifBuilder auteur(Auteur auteur);
  }

  public interface AnnulationDePresenceAEnregistrerMotifBuilder {
    AnnulationDePresenceAEnregistrer motif(MotifDAnnulation motif);
  }
}

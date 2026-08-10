package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record AnnulationAEnregistrer(SuiviDAtelierId suivi, EvenementDAtelierId evenement, Auteur auteur, MotifDAnnulation motif) {
  public AnnulationAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("evenement", evenement);
    Assert.notNull("auteur", auteur);
    Assert.notNull("motif", motif);
  }

  public static AnnulationAEnregistrerSuiviBuilder builder() {
    return suivi -> evenement -> auteur -> motif -> new AnnulationAEnregistrer(suivi, evenement, auteur, motif);
  }

  public interface AnnulationAEnregistrerSuiviBuilder {
    AnnulationAEnregistrerEvenementBuilder suivi(SuiviDAtelierId suivi);
  }

  public interface AnnulationAEnregistrerEvenementBuilder {
    AnnulationAEnregistrerAuteurBuilder evenement(EvenementDAtelierId evenement);
  }

  public interface AnnulationAEnregistrerAuteurBuilder {
    AnnulationAEnregistrerMotifBuilder auteur(Auteur auteur);
  }

  public interface AnnulationAEnregistrerMotifBuilder {
    AnnulationAEnregistrer motif(MotifDAnnulation motif);
  }
}

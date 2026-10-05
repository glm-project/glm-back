package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static com.glm.glmback.atelier.application.gestionanomalies.ResolutionFixture.*;

import com.glm.glmback.atelier.application.gestionanomalies.ContexteDeResolution;
import com.glm.glmback.atelier.application.gestionanomalies.PropositionAConfirmer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;

final class ConfirmationDeResolutionFixture {

  private ConfirmationDeResolutionFixture() {}

  static PropositionAConfirmer propositionDAnnulation(DonneesDApercu donnees) {
    var proposition = propositionDAnnulationDeTransition(donnees.suivi());
    return PropositionAConfirmer.builder()
      .commande(proposition.commande())
      .adresse(proposition.adresse())
      .revision(proposition.revision())
      .acte(proposition.acte())
      .evenement(proposition.evenement())
      .empreinteConsequences(donnees.empreinte());
  }

  static RestConfirmationAEnregistrer corps(PropositionAConfirmer proposition) {
    return new RestConfirmationAEnregistrer(
      proposition.commande(),
      new RestAdresseDossierConflit(proposition.adresse().suivi().uuid(), proposition.adresse().pointage().uuid()),
      proposition.revision().value(),
      RestActeDeResolution.from(proposition.acte()),
      proposition.empreinteConsequences(),
      proposition
        .evenement()
        .map(id -> id.uuid())
        .orElse(null)
    );
  }

  record DonneesDApercu(SuiviDAtelier suivi, ContexteDeResolution contexte, String empreinte) {}
}

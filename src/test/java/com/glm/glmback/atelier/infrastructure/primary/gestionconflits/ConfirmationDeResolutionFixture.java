package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;

import com.glm.glmback.atelier.application.gestionconflits.ContexteDeResolution;
import com.glm.glmback.atelier.application.gestionconflits.PreuveDApercu;
import com.glm.glmback.atelier.domain.SuiviDAtelier;

final class ConfirmationDeResolutionFixture {

  private ConfirmationDeResolutionFixture() {}

  static PreuveDApercu preuveDAnnulation(DonneesDApercu donnees) {
    var preuve = preuveDAnnulationDeTransition(donnees.suivi());
    return PreuveDApercu.builder()
      .commande(preuve.commande())
      .adresse(preuve.adresse())
      .revision(preuve.revision())
      .contexte(donnees.contexte())
      .acte(preuve.acte())
      .evenement(preuve.evenement())
      .evaluation(preuve.evaluation())
      .expireLe(preuve.expireLe())
      .empreinteConsequences(donnees.empreinte());
  }

  record DonneesDApercu(SuiviDAtelier suivi, ContexteDeResolution contexte, String empreinte) {}
}

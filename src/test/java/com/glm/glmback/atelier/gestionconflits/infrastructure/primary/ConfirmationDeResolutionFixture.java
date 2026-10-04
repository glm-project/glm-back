package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import static com.glm.glmback.atelier.gestionconflits.application.ResolutionFixture.*;

import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.gestionconflits.application.ContexteDeResolution;
import com.glm.glmback.atelier.gestionconflits.application.PreuveDApercu;

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

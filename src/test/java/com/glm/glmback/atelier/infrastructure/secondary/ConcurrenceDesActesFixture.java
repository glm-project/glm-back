package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.application.PreuveDApercu;
import java.util.UUID;

final class ConcurrenceDesActesFixture {

  private ConcurrenceDesActesFixture() {}

  static PreuveDApercu avecCommande(ChangementDeCommande donnees) {
    var preuve = donnees.preuve();
    return PreuveDApercu.builder()
      .commande(donnees.commande())
      .adresse(preuve.adresse())
      .revision(preuve.revision())
      .contexte(preuve.contexte())
      .acte(preuve.acte())
      .evenement(preuve.evenement())
      .evaluation(preuve.evaluation())
      .expireLe(preuve.expireLe())
      .empreinteConsequences(preuve.empreinteConsequences());
  }

  record ChangementDeCommande(PreuveDApercu preuve, UUID commande) {}
}

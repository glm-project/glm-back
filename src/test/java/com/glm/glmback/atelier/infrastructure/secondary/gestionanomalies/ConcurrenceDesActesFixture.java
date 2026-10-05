package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import com.glm.glmback.atelier.application.gestionanomalies.PropositionAConfirmer;
import java.util.UUID;

final class ConcurrenceDesActesFixture {

  private ConcurrenceDesActesFixture() {}

  static PropositionAConfirmer avecCommande(ChangementDeCommande donnees) {
    var proposition = donnees.proposition();
    return PropositionAConfirmer.builder()
      .commande(donnees.commande())
      .adresse(proposition.adresse())
      .revision(proposition.revision())
      .acte(proposition.acte())
      .evenement(proposition.evenement())
      .empreinteConsequences(proposition.empreinteConsequences());
  }

  static PropositionAConfirmer avecEmpreinte(PropositionAConfirmer proposition) {
    return PropositionAConfirmer.builder()
      .commande(proposition.commande())
      .adresse(proposition.adresse())
      .revision(proposition.revision())
      .acte(proposition.acte())
      .evenement(proposition.evenement())
      .empreinteConsequences("consequences-modifiees");
  }

  record ChangementDeCommande(PropositionAConfirmer proposition, UUID commande) {}
}

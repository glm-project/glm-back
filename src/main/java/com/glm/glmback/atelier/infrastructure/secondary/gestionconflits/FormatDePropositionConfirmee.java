package com.glm.glmback.atelier.infrastructure.secondary.gestionconflits;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.glm.glmback.atelier.application.gestionconflits.ContexteDeResolution;
import com.glm.glmback.atelier.application.gestionconflits.PropositionAConfirmer;
import com.glm.glmback.atelier.domain.gestionconflits.ActeDeResolution;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Representation de la proposition confirmee et de son contexte authentifie dans le recu. */
final class FormatDePropositionConfirmee {

  private static final ObjectMapper JSON = JsonMapper.builder().addMixIn(ActeDeResolution.class, TypesDActes.class).build();

  private FormatDePropositionConfirmee() {}

  static String serialise(PropositionAConfirmer proposition, ContexteDeResolution contexte) {
    return JSON.writeValueAsString(new PropositionConfirmee(proposition, contexte));
  }

  static PropositionConfirmee relit(String json) {
    return JSON.readValue(json, PropositionConfirmee.class);
  }

  record PropositionConfirmee(PropositionAConfirmer proposition, ContexteDeResolution contexte) {}

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
  @JsonSubTypes(
    {
      @JsonSubTypes.Type(value = ActeDeResolution.Annulation.class, name = "ANNULATION"),
      @JsonSubTypes.Type(value = ActeDeResolution.Correction.class, name = "CORRECTION"),
      @JsonSubTypes.Type(value = ActeDeResolution.Regularisation.class, name = "REGULARISATION"),
    }
  )
  private interface TypesDActes {}
}

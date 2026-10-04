package com.glm.glmback.atelier.gestionconflits.infrastructure.secondary;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.glm.glmback.atelier.gestionconflits.application.PreuveDApercu;
import com.glm.glmback.atelier.gestionconflits.domain.ActeDeResolution;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Format JSON partage par la reference authentifiee et le recu durable. */
final class FormatDePreuveDApercu {

  private static final ObjectMapper JSON = JsonMapper.builder().addMixIn(ActeDeResolution.class, TypesDActes.class).build();

  private FormatDePreuveDApercu() {}

  static String serialise(PreuveDApercu preuve) {
    return JSON.writeValueAsString(preuve);
  }

  static PreuveDApercu relit(String json) {
    return JSON.readValue(json, PreuveDApercu.class);
  }

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

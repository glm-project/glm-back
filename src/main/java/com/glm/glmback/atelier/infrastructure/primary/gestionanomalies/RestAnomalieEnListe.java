package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Une ligne de la liste, discriminee par la nature de l'anomalie. L'union n'existe que dans la reponse : le domaine
 * garde deux types de ligne distincts.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "nature")
@JsonSubTypes(
  {
    @JsonSubTypes.Type(value = RestConflitEnListe.class, name = "CONFLIT"),
    @JsonSubTypes.Type(value = RestFinAutomatiqueEnListe.class, name = "FIN_AUTOMATIQUE"),
  }
)
@Schema(
  discriminatorProperty = "nature",
  discriminatorMapping = {
    @DiscriminatorMapping(value = "CONFLIT", schema = RestConflitEnListe.class),
    @DiscriminatorMapping(value = "FIN_AUTOMATIQUE", schema = RestFinAutomatiqueEnListe.class),
  },
  oneOf = { RestConflitEnListe.class, RestFinAutomatiqueEnListe.class }
)
sealed interface RestAnomalieEnListe permits RestConflitEnListe, RestFinAutomatiqueEnListe {}

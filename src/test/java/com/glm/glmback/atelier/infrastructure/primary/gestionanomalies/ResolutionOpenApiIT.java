package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class ResolutionOpenApiIT {

  @Autowired
  private MockMvc rest;

  @Test
  void shouldDecrireLaPageDesAnomaliesCommeUneUnionDiscrimineeParLaNature() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$.components.schemas.RestPageDesAnomalies.properties.lignes.items.$ref").value("#/components/schemas/RestAnomalieEnListe")
      )
      .andExpect(jsonPath("$.components.schemas.RestAnomalieEnListe.discriminator.propertyName").value("nature"))
      .andExpect(
        jsonPath("$.components.schemas.RestAnomalieEnListe.discriminator.mapping.CONFLIT").value("#/components/schemas/RestConflitEnListe")
      )
      .andExpect(
        jsonPath("$.components.schemas.RestAnomalieEnListe.discriminator.mapping.FIN_AUTOMATIQUE").value(
          "#/components/schemas/RestFinAutomatiqueEnListe"
        )
      )
      .andExpect(
        jsonPath("$.components.schemas.RestAnomalieEnListe.oneOf[*].$ref").value(
          containsInAnyOrder("#/components/schemas/RestConflitEnListe", "#/components/schemas/RestFinAutomatiqueEnListe")
        )
      )
      .andExpect(jsonPath("$.components.schemas.RestConflitEnListe.allOf").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestFinAutomatiqueEnListe.allOf").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestConflitEnListe.properties.nature.enum[0]").value("CONFLIT"))
      .andExpect(jsonPath("$.components.schemas.RestFinAutomatiqueEnListe.properties.nature.enum[0]").value("FIN_AUTOMATIQUE"))
      .andExpect(
        jsonPath("$.components.schemas.RestConflitEnListe.required").value(
          containsInAnyOrder(
            "nature",
            "adresse",
            "revision",
            "elementId",
            "designation",
            "operateurId",
            "datePremierPointage",
            "nombrePointages"
          )
        )
      )
      .andExpect(
        jsonPath("$.components.schemas.RestFinAutomatiqueEnListe.required").value(
          containsInAnyOrder("nature", "adresse", "revision", "elementId", "designation", "operateurId", "activite", "debut", "echeance")
        )
      );
  }
}

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
  void shouldDecrireLesUnionsParLeursValeursWireSansHeritageRecursif() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$.components.schemas.RestActeDeResolution.discriminator.mapping.ANNULATION").value(
          "#/components/schemas/RestActeAnnulation"
        )
      )
      .andExpect(
        jsonPath("$.components.schemas.RestActeDeResolution.discriminator.mapping.CORRECTION").value(
          "#/components/schemas/RestActeCorrection"
        )
      )
      .andExpect(
        jsonPath("$.components.schemas.RestActeDeResolution.discriminator.mapping.REGULARISATION").value(
          "#/components/schemas/RestActeRegularisation"
        )
      )
      .andExpect(
        jsonPath("$.components.schemas.RestConfirmationDeResolution.discriminator.mapping.ENREGISTREE").value(
          "#/components/schemas/RestConfirmationEnregistree"
        )
      )
      .andExpect(
        jsonPath("$.components.schemas.RestConfirmationDeResolution.discriminator.mapping.NON_ATTESTEE").value(
          "#/components/schemas/RestConfirmationNonAttestee"
        )
      )
      .andExpect(jsonPath("$.components.schemas.RestActeAnnulation.allOf").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestActeCorrection.allOf").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestActeRegularisation.allOf").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestConfirmationEnregistree.allOf").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestConfirmationNonAttestee.allOf").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestActeAnnulation.required").value(containsInAnyOrder("kind", "pointage", "motif")))
      .andExpect(
        jsonPath("$.components.schemas.RestActeCorrection.required").value(containsInAnyOrder("kind", "pointage", "motif", "fait"))
      )
      .andExpect(jsonPath("$.components.schemas.RestActeRegularisation.required").value(containsInAnyOrder("kind", "fait")))
      .andExpect(jsonPath("$.components.schemas.RestConfirmationEnregistree.required").value(containsInAnyOrder("kind", "recu", "dossier")))
      .andExpect(jsonPath("$.components.schemas.RestConfirmationNonAttestee.required").value(containsInAnyOrder("kind")));
  }

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

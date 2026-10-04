package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

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
}

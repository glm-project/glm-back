package com.glm.glmback.wire.openapi.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class OpenApiConfigurationIT {

  @Autowired
  private MockMvc rest;

  @Test
  void shouldServirLaDescriptionSansJeton() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.info.title").value("glmproject API"))
      .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"));
  }

  @Test
  void shouldDecrireLesDeuxSurfacesDeLAtelier() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis'].post.tags[0]").value("Atelier - elements engages"))
      .andExpect(jsonPath("$.paths['/api/atelier/journees/pointages'].post.tags[0]").value("Atelier - presence des operateurs"))
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/temps-effectif'].get.summary").exists())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/cloture'].delete.summary").exists());
  }

  @Test
  @DisabledIfSystemProperty(
    named = OpenApiSpecification.REGENERATION_FLAG,
    matches = "true",
    disabledReason = "this run is rewriting the committed file, so the comparison would judge the version being replaced"
  )
  void shouldMatchTheCommittedSpecification() throws Exception {
    assertThat(OpenApiSpecification.served(rest))
      .describedAs(OpenApiSpecification.outOfDateMessage())
      .isEqualTo(OpenApiSpecification.committed());
  }
}

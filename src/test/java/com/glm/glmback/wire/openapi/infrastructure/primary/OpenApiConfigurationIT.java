package com.glm.glmback.wire.openapi.infrastructure.primary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La specification servie au developpeur front est un livrable : ce test la verifie comme tel.
 *
 * <p>
 * Il ne controle pas le detail des schemas, que springdoc deduit du code, mais ce qui casserait silencieusement son
 * usage : que la description soit publique sans jeton, que les deux surfaces de l'atelier y soient nommees, et que le
 * schema de securite y figure.
 * </p>
 */
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
}

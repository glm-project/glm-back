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

/**
 * La specification servie au developpeur front est un livrable : ce test la verifie comme tel.
 *
 * <p>
 * Il ne controle pas le detail des schemas, que springdoc deduit du code, mais ce qui casserait silencieusement son
 * usage : que la description soit publique sans jeton, que les deux surfaces de l'atelier y soient nommees, et que le
 * schema de securite y figure.
 * </p>
 *
 * <p>
 * Le fichier commite, lui, est confronte en entier : c'est lui que lit le front, et rien d'autre ne signalerait qu'une
 * signature a bouge sans qu'il suive.
 * </p>
 *
 * <p>
 * Les deux se partagent le travail plutot qu'ils ne se doublent. La comparaison entiere attrape tout, mais ne sait
 * dire que « ce n'est plus le meme document » ; les sondages, eux, nomment les proprietes dont la perte serait
 * silencieuse, et echouent en le disant. Le premier verifie de surcroit un acces : la description repond sans jeton,
 * ce que la comparaison ne saurait pas distinguer d'un document devenu different.
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

  @Test
  @DisabledIfSystemProperty(
    named = OpenApiSpecification.REGENERATION_FLAG,
    matches = "true",
    disabledReason = "the committed file is being rewritten by this very run"
  )
  void shouldMatchTheCommittedSpecification() throws Exception {
    assertThat(OpenApiSpecification.served(rest))
      .describedAs(OpenApiSpecification.outOfDateMessage())
      .isEqualTo(OpenApiSpecification.committed());
  }
}

package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.gestionconflits.domain.ConflitsDAtelier;
import com.glm.glmback.cucumber.CucumberSecurityConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
@SpringBootTest(
  classes = { GlmprojectApp.class, CucumberSecurityConfiguration.class },
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
class ListeDesConflitsAcquisitionIT {

  @Autowired
  private RestTestClient rest;

  @MockitoBean
  private ConflitsDAtelier conflits;

  @Test
  void shouldGarderUnEchecDAcquisitionCommeErreurHttp() {
    when(conflits.list(any(), any())).thenThrow(new IllegalStateException("acquisition interrompue"));
    String token = Base64.getEncoder().encodeToString("lecteur|ROLE_USER|impeccmold".getBytes(StandardCharsets.UTF_8));

    rest
      .get()
      .uri("/api/atelier/conflits")
      .header("Authorization", "Bearer " + token)
      .exchange()
      .expectStatus()
      .isEqualTo(500)
      .expectBody()
      .jsonPath("$.complete")
      .doesNotExist();
  }
}

package com.glm.glmback.atelier.infrastructure.primary;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest(
  properties = {
    "application.multitenancy.tenants[0].id=supervision_fixture",
    "application.multitenancy.tenants[0].schema=supervision_fixture",
    "application.multitenancy.tenants[1].id=supervision_voisine",
    "application.multitenancy.tenants[1].schema=supervision_voisine",
  }
)
@AutoConfigureMockMvc
class SupervisionDAtelierResourceIT {

  @Autowired
  private MockMvc rest;

  @MockitoBean
  private Clock clock;

  @Test
  @WithTenant("supervision_fixture")
  void shouldReadAnEmptySupervisionAtTheServerEvaluation() throws Exception {
    when(clock.now()).thenReturn(Instant.parse("2026-09-13T10:00:00Z"));

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.evaluation").value("2026-09-13T10:00:00Z"))
      .andExpect(jsonPath("$.operateurs").isEmpty())
      .andExpect(jsonPath("$.activites").isEmpty())
      .andExpect(jsonPath("$.sequencesEnConflit").isEmpty());
  }
}

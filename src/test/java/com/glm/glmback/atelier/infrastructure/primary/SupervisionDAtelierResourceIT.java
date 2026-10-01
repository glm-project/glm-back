package com.glm.glmback.atelier.infrastructure.primary;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

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

  @Autowired
  private EntityManager entities;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanupFixture() {
    TenantSecurityContexts.authenticateOn("supervision_fixture");
    transactions.executeWithoutResult(status -> entities.createNativeQuery("delete from operateur").executeUpdate());
  }

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

  @Test
  @WithTenant("supervision_fixture")
  void shouldKeepAnOperatorWithoutActivityOrTrades() throws Exception {
    UUID id = UUID.fromString("9d0bed2e-201f-4b2b-ae62-c6c09bf1f007");
    when(clock.now()).thenReturn(Instant.parse("2026-09-13T10:00:00Z"));
    transactions.executeWithoutResult(status ->
      entities
        .createNativeQuery("insert into operateur (id, nom, prenom) values (:id, 'Leroy', 'Camille')")
        .setParameter("id", id)
        .executeUpdate()
    );

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.operateurs.length()").value(1))
      .andExpect(jsonPath("$.operateurs[0].id").value(id.toString()))
      .andExpect(jsonPath("$.operateurs[0].nom").value("Leroy"))
      .andExpect(jsonPath("$.operateurs[0].prenom").value("Camille"))
      .andExpect(jsonPath("$.operateurs[0].metiers").isEmpty())
      .andExpect(jsonPath("$.activites").isEmpty());
  }
}

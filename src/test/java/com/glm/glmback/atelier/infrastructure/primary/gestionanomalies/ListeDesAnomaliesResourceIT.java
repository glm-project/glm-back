package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@AutoConfigureMockMvc
class ListeDesAnomaliesResourceIT {

  @Autowired
  private MockMvc rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("impeccmold")
  void shouldRendreUneFinAutomatiqueAvecSonActiviteEtSonEcheanceSansDuree() throws Exception {
    Instant debut = Instant.parse("2026-01-12T08:00:00.123456789Z");
    var ouvrant = debutSurFraiseuse1ParDupontA(debut);
    var suivi = suiviEngageLe1erJanvier2025Pour(elementDeFinAutomatiqueNomme("FINAUTO_RESSOURCE_2026")).enregistre(ouvrant);
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));

    rest
      .perform(get("/api/atelier/anomalies").param("element", suivi.element().id().uuid().toString()).with(lecteur()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalElementsCount").value(1))
      .andExpect(jsonPath("$.currentPage").value(0))
      .andExpect(jsonPath("$.pageSize").value(20))
      .andExpect(jsonPath("$.content[0].nature").doesNotExist())
      .andExpect(jsonPath("$.content[0].adresse.suivi").value(suivi.id().uuid().toString()))
      .andExpect(jsonPath("$.content[0].adresse.pointage").value(ouvrant.id().uuid().toString()))
      .andExpect(jsonPath("$.content[0].activite").value(ouvrant.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.content[0].revision").value(0))
      .andExpect(jsonPath("$.content[0].elementId").value(suivi.element().id().uuid().toString()))
      .andExpect(jsonPath("$.content[0].designation").value("FINAUTO_RESSOURCE_2026"))
      .andExpect(jsonPath("$.content[0].operateurId").value(ouvrant.operateur().uuid().toString()))
      .andExpect(jsonPath("$.content[0].posteId").value(ouvrant.poste().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.content[0].debut").value("2026-01-12T08:00:00.123456789Z"))
      .andExpect(jsonPath("$.content[0].echeance").value("2026-01-12T21:00:00.123456789Z"))
      .andExpect(jsonPath("$.content[0].duree").doesNotExist())
      .andExpect(jsonPath("$.content[0].journal").doesNotExist());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldLaisserLEnCoursHorsDeLaListe() throws Exception {
    var ouvrant = debutSurFraiseuse1ParDupontA(Instant.now().minusSeconds(3600));
    var suivi = suiviEngageLe1erJanvier2025Pour(elementDeFinAutomatiqueNomme("FINAUTO_EN_COURS_2026")).enregistre(ouvrant);
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));

    rest
      .perform(get("/api/atelier/anomalies").param("element", suivi.element().id().uuid().toString()).with(lecteur()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalElementsCount").value(0))
      .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldReserverLaListeAuxLecteursEtGestionnaires() throws Exception {
    rest
      .perform(
        get("/api/atelier/anomalies")
          .param("element", "element-certainement-absent-2043")
          .with(
            jwt()
              .jwt(token -> token.claim("tenant", "impeccmold"))
              .authorities(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalElementsCount").value(0))
      .andExpect(jsonPath("$.content").isEmpty());

    rest
      .perform(
        get("/api/atelier/anomalies").with(
          jwt()
            .jwt(token -> token.claim("tenant", "impeccmold"))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
        )
      )
      .andExpect(status().isForbidden());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldSupprimerSansRedirectionLAncienneRouteDesConflits() throws Exception {
    rest.perform(get("/api/atelier/conflits").with(lecteur())).andExpect(status().isNotFound());
    rest
      .perform(get("/api/atelier/suivis/{suivi}/conflits/{pointage}", UUID.randomUUID(), UUID.randomUUID()).with(lecteur()))
      .andExpect(status().isNotFound())
      .andExpect(header().doesNotExist("Location"));
    rest
      .perform(
        post("/api/atelier/suivis/{suivi}/conflits/{pointage}/apercus", UUID.randomUUID(), UUID.randomUUID())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
          .with(lecteur())
      )
      .andExpect(status().isNotFound());
  }

  private static RequestPostProcessor lecteur() {
    return jwt()
      .jwt(token -> token.claim("tenant", "impeccmold"))
      .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }
}

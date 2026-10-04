package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.gestionconflits.domain.ConflitsFixture.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@AutoConfigureMockMvc
class ListeDesConflitsResourceIT {

  @Autowired
  private MockMvc rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("impeccmold")
  void shouldRendreUneLigneAutoritaireSansJournalAuxOperateurs() throws Exception {
    Instant debut = Instant.parse("2043-01-11T08:00:00.123456789Z");
    var ouvrant = debutSurFraiseuse1ParDupontA(debut);
    var suivi = suiviOF2026000042EngageA(debut)
      .enregistre(ouvrant)
      .enregistre(finDe(ouvrant).a(debut.plusSeconds(3600)))
      .enregistre(finDe(ouvrant).a(debut.plusSeconds(7200)));
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));

    rest
      .perform(
        get("/api/atelier/conflits")
          .param("element", suivi.element().id().uuid().toString())
          .with(
            jwt()
              .jwt(token -> token.claim("tenant", "impeccmold"))
              .authorities(new SimpleGrantedAuthority("ROLE_USER"))
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.complete").value(true))
      .andExpect(jsonPath("$.total").value(1))
      .andExpect(jsonPath("$.page").value(0))
      .andExpect(jsonPath("$.size").value(20))
      .andExpect(jsonPath("$.lignes[0].adresse.suivi").value(suivi.id().uuid().toString()))
      .andExpect(jsonPath("$.lignes[0].adresse.pointage").value(ouvrant.id().uuid().toString()))
      .andExpect(jsonPath("$.lignes[0].revision").value(0))
      .andExpect(jsonPath("$.lignes[0].elementId").value(suivi.element().id().uuid().toString()))
      .andExpect(jsonPath("$.lignes[0].designation").value(suivi.element().nom().value()))
      .andExpect(jsonPath("$.lignes[0].operateurId").value(ouvrant.operateur().uuid().toString()))
      .andExpect(jsonPath("$.lignes[0].posteId").value(ouvrant.poste().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.lignes[0].datePremierPointage").value(debut.toString()))
      .andExpect(jsonPath("$.lignes[0].nombrePointages").value(3))
      .andExpect(jsonPath("$.lignes[0].journal").doesNotExist());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldDistinguerUneLectureVideCompleteDUneAbsenceDeDroitMetier() throws Exception {
    rest
      .perform(
        get("/api/atelier/conflits")
          .param("element", "element-certainement-absent-2043")
          .with(
            jwt()
              .jwt(token -> token.claim("tenant", "impeccmold"))
              .authorities(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.complete").value(true))
      .andExpect(jsonPath("$.total").value(0))
      .andExpect(jsonPath("$.lignes").isEmpty());

    rest
      .perform(
        get("/api/atelier/conflits").with(
          jwt()
            .jwt(token -> token.claim("tenant", "impeccmold"))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
        )
      )
      .andExpect(status().isForbidden());
  }
}

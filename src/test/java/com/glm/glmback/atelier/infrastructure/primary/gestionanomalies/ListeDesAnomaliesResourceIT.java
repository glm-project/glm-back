package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.ConflitsFixture.*;
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
        get("/api/atelier/anomalies")
          .param("nature", "CONFLIT")
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
        get("/api/atelier/anomalies")
          .param("nature", "CONFLIT")
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
        get("/api/atelier/anomalies")
          .param("nature", "CONFLIT")
          .with(
            jwt()
              .jwt(token -> token.claim("tenant", "impeccmold"))
              .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
          )
      )
      .andExpect(status().isForbidden());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneNatureAbsenteAvecUnCodeStable() throws Exception {
    rest
      .perform(get("/api/atelier/anomalies").with(lecteur()))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.type").value("urn:glm:erreur:atelier:nature-d-anomalie-invalide"))
      .andExpect(jsonPath("$.title").value("nature d'anomalie invalide"))
      .andExpect(jsonPath("$.status").value(400))
      .andExpect(jsonPath("$.message").value("La nature d'anomalie est obligatoire. Valeurs possibles : CONFLIT."))
      .andExpect(jsonPath("$.lignes").doesNotExist());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUneNatureInconnueAvecUnCodeStable() throws Exception {
    rest
      .perform(get("/api/atelier/anomalies").param("nature", "INCONNUE").with(lecteur()))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.type").value("urn:glm:erreur:atelier:nature-d-anomalie-invalide"))
      .andExpect(jsonPath("$.status").value(400))
      .andExpect(jsonPath("$.message").value("La nature d'anomalie 'INCONNUE' est inconnue. Valeurs possibles : CONFLIT."));
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

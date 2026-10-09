package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.infrastructure.secondary.NaturesDesFixtures;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@AutoConfigureMockMvc
class DossierAnomalieResourceIT {

  @Autowired
  private MockMvc rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entities;

  @MockitoBean
  private Clock clock;

  @BeforeEach
  void declarerLesNaturesDesFixtures() {
    NaturesDesFixtures.declarer(entities, transactions, "dossier_fixture");
  }

  @Test
  @WithTenant("dossier_fixture")
  void shouldLireLeDossierDUneFinAutomatiqueEtLesPointagesDeSaCle() throws Exception {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var autreOperateur = debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(autreOperateur);
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T22:00:00Z"));

    rest
      .perform(get("/api/atelier/suivis/{suivi}/anomalies/{pointage}", suivi.id().uuid(), travail.id().uuid()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.adresse.suivi").value(suivi.id().uuid().toString()))
      .andExpect(jsonPath("$.adresse.pointage").value(travail.id().uuid().toString()))
      .andExpect(jsonPath("$.revision").value(0))
      .andExpect(jsonPath("$.evaluation").value("2026-05-10T22:00:00Z"))
      .andExpect(jsonPath("$.activite.activite").value(travail.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.activite.fin").value("2026-05-10T21:00:00Z"))
      .andExpect(jsonPath("$.activite.duree").value("PT13H"))
      .andExpect(jsonPath("$.pointages.length()").value(1))
      .andExpect(jsonPath("$.pointages[0].id").value(travail.id().uuid().toString()))
      .andExpect(jsonPath("$.kind").doesNotExist())
      .andExpect(jsonPath("$.suivi").doesNotExist());
  }

  @Test
  @WithTenant("dossier_fixture")
  void shouldRepondre404QuandLActiviteNEstPasEncoreEchue() throws Exception {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T20:59:59Z"));

    rest
      .perform(get("/api/atelier/suivis/{suivi}/anomalies/{pointage}", suivi.id().uuid(), travail.id().uuid()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.type").value("urn:glm:erreur:atelier:fin-automatique-introuvable"));
  }

  @Test
  @WithTenant("dossier_fixture")
  void shouldRepondre404PourUnSuiviInconnu() throws Exception {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    rest
      .perform(get("/api/atelier/suivis/{suivi}/anomalies/{pointage}", SuiviDAtelierId.newId().uuid(), travail.id().uuid()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.type").value("urn:glm:erreur:atelier:suivi-d-atelier-introuvable"));
  }
}

package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest(
  properties = { "application.multitenancy.tenants[0].id=apercu_fixture", "application.multitenancy.tenants[0].schema=apercu_fixture" }
)
@AutoConfigureMockMvc
class ApercuDeResolutionResourceIT {

  @Autowired
  private MockMvc rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @MockitoBean
  private Clock clock;

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  @WithTenant("apercu_fixture")
  void shouldProposerLAnnulationAvecTouteAncreActiveDeLaSequence(boolean ancreAlternative) throws Exception {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnTravailDe(travail).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(transition);
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_12H.plusSeconds(3600));
    var commande = UUID.randomUUID();
    var ancre = ancreAlternative ? travail.id() : transition.id();
    rest
      .perform(
        post("/api/atelier/suivis/{suivi}/anomalies/{pointage}/apercus", suivi.id().uuid(), ancre.uuid())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            """
            {"commande":"%s","revision":0,"acte":{"kind":"ANNULATION","pointage":"%s","motif":"saisie incorrecte"}}
            """.formatted(commande, transition.id().uuid())
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.commande").value(commande.toString()))
      .andExpect(jsonPath("$.adresse.pointage").value(ancre.uuid().toString()))
      .andExpect(jsonPath("$.empreinteConsequences").value(org.hamcrest.Matchers.matchesPattern("[a-f0-9]{64}")))
      .andExpect(jsonPath("$.evenement").doesNotExist())
      .andExpect(jsonPath("$.reference").doesNotExist())
      .andExpect(jsonPath("$.expireLe").doesNotExist())
      .andExpect(jsonPath("$.acte.kind").value("ANNULATION"))
      .andExpect(jsonPath("$.acte.motif").value("saisie incorrecte"))
      .andExpect(jsonPath("$.avant.kind").value("EN_CONFLIT"))
      .andExpect(jsonPath("$.apres.kind").value(ancreAlternative ? "SANS_ANOMALIE" : "ANCRE_ANNULEE"))
      .andExpect(jsonPath("$.apres.perimetre.nombrePointages").value(2))
      .andExpect(jsonPath("$.apres.activites[0].etat").value("EN_COURS"))
      .andExpect(jsonPath("$.apres.activites[0].duree").doesNotExist());
    TenantSecurityContexts.authenticateOn("apercu_fixture");
    var relu = transactions.execute(status -> suivis.get(suivi.id()).orElseThrow());
    assertThat(relu).isEqualTo(suivi);
  }
}

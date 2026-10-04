package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.gestionconflits.application.ResolutionFixture.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.gestionconflits.application.PreparationDesActes;
import com.glm.glmback.atelier.gestionconflits.application.ReferencesDApercu;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest(
  properties = {
    "application.multitenancy.tenants[0].id=confirmations_fixture", "application.multitenancy.tenants[0].schema=confirmations_fixture",
  }
)
@AutoConfigureMockMvc
class ConfirmationDeResolutionResourceIT {

  @Autowired
  private MockMvc rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private ReferencesDApercu references;

  @Autowired
  private PreparationDesActes preparation;

  @MockitoBean
  private Clock clock;

  @Test
  @WithTenant("confirmations_fixture")
  void shouldRendreUneIssueNonAttesteeQuandAucunRecuNestVisible() throws Exception {
    // WHEN THEN
    rest
      .perform(get("/api/atelier/suivis/{suivi}/confirmations-de-resolution/{commande}", UUID.randomUUID(), UUID.randomUUID()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("NON_ATTESTEE"))
      .andExpect(jsonPath("$.recu").doesNotExist())
      .andExpect(jsonPath("$.dossier").doesNotExist());
  }

  @Test
  @WithTenant("confirmations_fixture")
  void shouldConfirmerUneReferenceAuthentifieeEtRendreLeRecuAvecLeDossierActuel() throws Exception {
    // GIVEN
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    var suivi = transactions.execute(status -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var contexte = GestionnaireConnecte.get();
    var base = preuveDAnnulationDeTransition(suivi);
    var prepare = preparation.prepare(suivi, base.acte(), base.evenement(), contexte.gestionnaire().auteur(), clock.now());
    var preuve = ConfirmationDeResolutionFixture.preuveDAnnulation(
      new ConfirmationDeResolutionFixture.DonneesDApercu(suivi, contexte, prepare.empreinteConsequences())
    );
    var reference = references.issue(preuve);
    var corps = JsonMapper.builder().build().writeValueAsString(Map.of("commande", preuve.commande(), "reference", reference));
    // WHEN THEN
    rest
      .perform(
        post("/api/atelier/suivis/{suivi}/confirmations-de-resolution", suivi.id().uuid())
          .contentType(MediaType.APPLICATION_JSON)
          .content(corps)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("ENREGISTREE"))
      .andExpect(jsonPath("$.recu.commande").value(preuve.commande().toString()))
      .andExpect(jsonPath("$.recu.adresse.pointage").value(preuve.adresse().pointage().uuid().toString()))
      .andExpect(jsonPath("$.recu.acte.kind").value("ANNULATION"))
      .andExpect(jsonPath("$.recu.revisionDeDepart").value(0))
      .andExpect(jsonPath("$.recu.revisionEnregistree").value(1))
      .andExpect(jsonPath("$.recu.enregistreLe").value("2026-05-10T17:00:00Z"))
      .andExpect(jsonPath("$.recu.evenementsTouches[0]").value(preuve.adresse().pointage().uuid().toString()))
      .andExpect(jsonPath("$.dossier.kind").value("ANCRE_ANNULEE"))
      .andExpect(jsonPath("$.dossier.revision").value(1))
      .andExpect(jsonPath("$.dossier.activites.length()").value(1));
  }

  @Test
  @WithTenant("confirmations_fixture")
  void shouldRetrouverLeRecuEtRejouerAvecLeDossierActualiseApresUneAutreEcriture() throws Exception {
    // GIVEN
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    var suivi = transactions.execute(status -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var contexte = GestionnaireConnecte.get();
    var base = preuveDAnnulationDeTransition(suivi);
    var prepare = preparation.prepare(suivi, base.acte(), base.evenement(), contexte.gestionnaire().auteur(), clock.now());
    var preuve = ConfirmationDeResolutionFixture.preuveDAnnulation(
      new ConfirmationDeResolutionFixture.DonneesDApercu(suivi, contexte, prepare.empreinteConsequences())
    );
    var corps = JsonMapper.builder()
      .build()
      .writeValueAsString(Map.of("commande", preuve.commande(), "reference", references.issue(preuve)));
    rest
      .perform(
        post("/api/atelier/suivis/{suivi}/confirmations-de-resolution", suivi.id().uuid())
          .contentType(MediaType.APPLICATION_JSON)
          .content(corps)
      )
      .andExpect(status().isOk());
    rest
      .perform(put("/api/atelier/suivis/{suivi}/cloture", suivi.id().uuid()).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    // WHEN THEN
    rest
      .perform(get("/api/atelier/suivis/{suivi}/confirmations-de-resolution/{commande}", suivi.id().uuid(), preuve.commande()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("ENREGISTREE"))
      .andExpect(jsonPath("$.recu.commande").value(preuve.commande().toString()))
      .andExpect(jsonPath("$.recu.revisionEnregistree").value(1))
      .andExpect(jsonPath("$.dossier.revision").value(2))
      .andExpect(jsonPath("$.dossier.activites.length()").value(1));
    rest
      .perform(
        post("/api/atelier/suivis/{suivi}/confirmations-de-resolution", suivi.id().uuid())
          .contentType(MediaType.APPLICATION_JSON)
          .content(corps)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("ENREGISTREE"))
      .andExpect(jsonPath("$.recu.revisionEnregistree").value(1))
      .andExpect(jsonPath("$.dossier.revision").value(2))
      .andExpect(jsonPath("$.dossier.activites.length()").value(1));
  }
}

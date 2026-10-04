package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.gestionconflits.PreparationDesActes;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
  void shouldConfirmerUnePropositionExpliciteEtRendreLeRecuAvecLeDossierActuel() throws Exception {
    // GIVEN
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    var suivi = transactions.execute(status -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var contexte = GestionnaireConnecte.get();
    var base = propositionDAnnulationDeTransition(suivi);
    var prepare = preparation.prepare(suivi, base.acte(), base.evenement(), contexte.gestionnaire().auteur(), clock.now());
    var proposition = ConfirmationDeResolutionFixture.propositionDAnnulation(
      new ConfirmationDeResolutionFixture.DonneesDApercu(suivi, contexte, prepare.empreinteConsequences())
    );
    var corps = JsonMapper.builder().build().writeValueAsString(ConfirmationDeResolutionFixture.corps(proposition));
    // WHEN THEN
    rest
      .perform(
        post("/api/atelier/suivis/{suivi}/confirmations-de-resolution", suivi.id().uuid())
          .contentType(MediaType.APPLICATION_JSON)
          .content(corps)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("ENREGISTREE"))
      .andExpect(jsonPath("$.recu.commande").value(proposition.commande().toString()))
      .andExpect(jsonPath("$.recu.adresse.pointage").value(proposition.adresse().pointage().uuid().toString()))
      .andExpect(jsonPath("$.recu.acte.kind").value("ANNULATION"))
      .andExpect(jsonPath("$.recu.revisionDeDepart").value(0))
      .andExpect(jsonPath("$.recu.revisionEnregistree").value(1))
      .andExpect(jsonPath("$.recu.enregistreLe").value("2026-05-10T17:00:00Z"))
      .andExpect(jsonPath("$.recu.evenementsTouches[0]").value(proposition.adresse().pointage().uuid().toString()))
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
    var base = propositionDAnnulationDeTransition(suivi);
    var prepare = preparation.prepare(suivi, base.acte(), base.evenement(), contexte.gestionnaire().auteur(), clock.now());
    var proposition = ConfirmationDeResolutionFixture.propositionDAnnulation(
      new ConfirmationDeResolutionFixture.DonneesDApercu(suivi, contexte, prepare.empreinteConsequences())
    );
    var corps = JsonMapper.builder().build().writeValueAsString(ConfirmationDeResolutionFixture.corps(proposition));
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
      .perform(get("/api/atelier/suivis/{suivi}/confirmations-de-resolution/{commande}", suivi.id().uuid(), proposition.commande()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("ENREGISTREE"))
      .andExpect(jsonPath("$.recu.commande").value(proposition.commande().toString()))
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

  @Test
  @WithTenant("confirmations_fixture")
  void shouldReprendreLaReponsePerdueApresUnChangementDuNomDAffichageSansReecrireLAuteur() throws Exception {
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);
    var suivi = transactions.execute(status -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var contexte = GestionnaireConnecte.get();
    var base = propositionDAnnulationDeTransition(suivi);
    var prepare = preparation.prepare(suivi, base.acte(), base.evenement(), contexte.gestionnaire().auteur(), clock.now());
    var proposition = ConfirmationDeResolutionFixture.propositionDAnnulation(
      new ConfirmationDeResolutionFixture.DonneesDApercu(suivi, contexte, prepare.empreinteConsequences())
    );
    var corps = JsonMapper.builder().build().writeValueAsString(ConfirmationDeResolutionFixture.corps(proposition));
    var premier = rest
      .perform(
        post("/api/atelier/suivis/{suivi}/confirmations-de-resolution", suivi.id().uuid())
          .contentType(MediaType.APPLICATION_JSON)
          .content(corps)
      )
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H.plusSeconds(3600));
    var reprise = rest
      .perform(
        post("/api/atelier/suivis/{suivi}/confirmations-de-resolution", suivi.id().uuid())
          .with(
            jwt()
              .jwt(token ->
                token
                  .issuer(contexte.gestionnaire().emetteur())
                  .subject(contexte.gestionnaire().sujet())
                  .claim("preferred_username", "gestionnaire-renomme")
                  .claim("tenant", "confirmations_fixture")
              )
              .authorities(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))
          )
          .contentType(MediaType.APPLICATION_JSON)
          .content(corps)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dossier.revision").value(1))
      .andReturn()
      .getResponse()
      .getContentAsString();
    var json = JsonMapper.builder().build();
    assertThat(json.readTree(reprise).path("recu")).isEqualTo(json.readTree(premier).path("recu"));
    com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts.authenticateOn("confirmations_fixture");
    var historique = transactions.execute(status -> suivis.get(suivi.id()).orElseThrow());
    assertThat(
      historique.journal().evenement(proposition.adresse().pointage()).orElseThrow().annulation().orElseThrow().auteur()
    ).isEqualTo(contexte.gestionnaire().auteur());
  }
}

package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest(
  properties = { "application.multitenancy.tenants[0].id=dossier_fixture", "application.multitenancy.tenants[0].schema=dossier_fixture" }
)
@AutoConfigureMockMvc
class DossierAnomalieResourceIT {

  @Autowired
  private MockMvc rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @MockitoBean
  private Clock clock;

  @Test
  @WithTenant("dossier_fixture")
  void shouldLireUnDossierParUnAncrageActifCommeUtilisateur() throws Exception {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nc = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nc).enregistre(fin);
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_17H);

    rest
      .perform(get("/api/atelier/suivis/{suivi}/anomalies/{pointage}", suivi.id().uuid(), fin.id().uuid()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("EN_CONFLIT"))
      .andExpect(jsonPath("$.adresse.suivi").value(suivi.id().uuid().toString()))
      .andExpect(jsonPath("$.adresse.pointage").value(fin.id().uuid().toString()))
      .andExpect(jsonPath("$.revision").value(0))
      .andExpect(jsonPath("$.evaluation").value("2026-05-10T17:00:00Z"))
      .andExpect(jsonPath("$.suivi.journal.length()").value(3))
      .andExpect(jsonPath("$.diagnostics[0].raison").value("CIBLE_REMPLACEE"))
      .andExpect(jsonPath("$.diagnostics[0].cible.termineePar").value(nc.id().uuid().toString()))
      .andExpect(jsonPath("$.activites.length()").value(2))
      .andExpect(jsonPath("$.activites[0].etat").value("A_RESOUDRE"))
      .andExpect(jsonPath("$.activites[0].duree").doesNotExist());
  }

  @Test
  @WithTenant("dossier_fixture")
  void shouldLireLeDossierDUneFinAutomatiqueSansSequenceAvecSaRegularisationSansHeure() throws Exception {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T22:00:00Z"));

    rest
      .perform(get("/api/atelier/suivis/{suivi}/anomalies/{pointage}", suivi.id().uuid(), travail.id().uuid()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("FIN_AUTOMATIQUE"))
      .andExpect(jsonPath("$.finAutomatique").value(true))
      .andExpect(jsonPath("$.enConflit").value(false))
      .andExpect(jsonPath("$.sequence").doesNotExist())
      .andExpect(jsonPath("$.perimetre.pointages[0]").value(travail.id().uuid().toString()))
      .andExpect(jsonPath("$.activites[0].activite").value(travail.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.activites[0].etat").value("ECHUE"))
      .andExpect(jsonPath("$.activites[0].fin").value("2026-05-10T21:00:00Z"))
      .andExpect(jsonPath("$.activites[0].duree").value("PT13H"))
      .andExpect(jsonPath("$.choix.length()").value(1))
      .andExpect(jsonPath("$.choix[0].code").value("REGULARISER_FIN"))
      .andExpect(jsonPath("$.choix[0].kind").value("REGULARISATION"))
      .andExpect(jsonPath("$.choix[0].pointage").value(travail.id().uuid().toString()))
      .andExpect(jsonPath("$.choix[0].fait.type").value("FIN"))
      .andExpect(jsonPath("$.choix[0].fait.intention").value("FIN"))
      .andExpect(jsonPath("$.choix[0].fait.activiteVisee").value(travail.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.choix[0].fait.operateur").value(OPERATEUR_ID_DUPONT.uuid().toString()))
      .andExpect(jsonPath("$.choix[0].fait.poste").value(POSTE_ID_FRAISEUSE_1.uuid().toString()))
      .andExpect(jsonPath("$.choix[0].fait.instant").doesNotExist());
  }

  @Test
  @WithTenant("dossier_fixture")
  void shouldProposerLaCorrectionDeLaFinTardiveAvecSonHeureReprise() throws Exception {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(Instant.parse("2026-05-10T23:00:00Z"));
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T23:30:00Z"));

    rest
      .perform(get("/api/atelier/suivis/{suivi}/anomalies/{pointage}", suivi.id().uuid(), travail.id().uuid()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kind").value("FIN_AUTOMATIQUE"))
      .andExpect(jsonPath("$.choix.length()").value(1))
      .andExpect(jsonPath("$.choix[0].code").value("CORRIGER_FIN_TARDIVE"))
      .andExpect(jsonPath("$.choix[0].kind").value("CORRECTION"))
      .andExpect(jsonPath("$.choix[0].pointage").value(fin.id().uuid().toString()))
      .andExpect(jsonPath("$.choix[0].fait.activiteVisee").value(travail.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.choix[0].fait.instant").value("2026-05-10T23:00:00Z"));
  }
}

package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@AutoConfigureMockMvc
class SupervisionDAtelierResourceIT {

  @Autowired
  private MockMvc rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private JourneeDeTravailRepository journees;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("impeccmold")
  void shouldListConflictWithoutActivityOrResolvedOperator() throws Exception {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debut)
      .enregistre(finDe(debut).a(LE_10_MAI_2026_A_9H))
      .annule(debut.id(), new Annulation(AUTEUR_LEROY, LE_10_MAI_2026_A_17H, MOTIF_ERREUR_DE_SAISIE));
    transactions.executeWithoutResult(status -> suivis.create(suivi));

    rest
      .perform(
        get("/api/atelier/suivis")
          .param("etats", "EN_COURS")
          .param("inclureConflits", "true")
          .param("debut", LE_10_MAI_2026_A_7H.toString())
          .param("fin", LE_10_MAI_2026_A_7H.toString())
          .param("size", "100")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[?(@.id == '" + suivi.id().uuid() + "')].conflits[0].activites[0]").doesNotExist())
      .andExpect(jsonPath("$.content[?(@.id == '" + suivi.id().uuid() + "')].conflits[0].operateur").doesNotExist())
      .andExpect(jsonPath("$.content[?(@.id == '" + suivi.id().uuid() + "')].conflits[0].pointages[0]").exists())
      .andExpect(jsonPath("$.content[?(@.id == '" + suivi.id().uuid() + "')].evaluation").exists());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldOnlyListOpenVenuesEvenWhenTheirArrivalWasLongAgo() throws Exception {
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    JourneeDeTravail ouverte = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), operateur).enregistre(
      arriveeDeDupontA(LE_10_MAI_2026_A_7H)
    );
    JourneeDeTravail vide = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), operateur);
    transactions.executeWithoutResult(status -> {
      journees.create(ouverte);
      journees.create(vide);
    });
    rest
      .perform(get("/api/atelier/journees").param("etat", "PRESENT").param("operateur", operateur.uuid().toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(1))
      .andExpect(jsonPath("$.content[0].id").value(ouverte.id().uuid().toString()))
      .andExpect(jsonPath("$.content[0].fenetres[0].debut").value(LE_10_MAI_2026_A_7H.toString()));
  }
}

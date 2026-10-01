package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.*;
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

  @Autowired
  private SuiviDAtelierRepository suivis;

  @AfterEach
  void cleanupFixture() {
    TenantSecurityContexts.authenticateOn("supervision_fixture");
    transactions.executeWithoutResult(status -> {
      entities.createNativeQuery("delete from activite_d_atelier").executeUpdate();
      entities.createNativeQuery("delete from pointage_en_conflit").executeUpdate();
      entities.createNativeQuery("delete from sequence_en_conflit").executeUpdate();
      entities.createNativeQuery("delete from evenement_d_atelier").executeUpdate();
      entities.createNativeQuery("delete from suivi_d_atelier").executeUpdate();
      entities.createNativeQuery("delete from element_de_fabrication").executeUpdate();
      entities.createNativeQuery("delete from operateur_poste").executeUpdate();
      entities.createNativeQuery("delete from poste_de_travail").executeUpdate();
      entities.createNativeQuery("delete from operateur").executeUpdate();
    });
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
  void shouldReadAWorkActivityOnAnOrderWithItsStableIdentityAndDeadline() throws Exception {
    var ouverture = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(ouverture);
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> suivis.create(suivi));

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.activites[0].id").value(ouverture.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.activites[0].operateurId").value(OPERATEUR_ID_DUPONT.uuid().toString()))
      .andExpect(jsonPath("$.activites[0].categorie").value("TRAVAIL"))
      .andExpect(jsonPath("$.activites[0].element.id").value(ELEMENT_OF_2026_000042.uuid().toString()))
      .andExpect(jsonPath("$.activites[0].element.type").value("ORDRE_DE_FABRICATION"))
      .andExpect(jsonPath("$.activites[0].element.nom").value(NOM_OF_2026_000042.value()))
      .andExpect(jsonPath("$.activites[0].element.reference").doesNotExist())
      .andExpect(jsonPath("$.activites[0].poste").doesNotExist())
      .andExpect(jsonPath("$.activites[0].debut").value("2026-05-10T08:00:00Z"))
      .andExpect(jsonPath("$.activites[0].echeance").value("2026-05-10T21:00:00Z"))
      .andExpect(jsonPath("$.activites[0].etat").value("EN_COURS"))
      .andExpect(jsonPath("$.activites[0].finRetenue").doesNotExist());
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldDescribeANonConformityOnAReferencedMoldAndItsPost() throws Exception {
    var element = new ElementEngage(ELEMENT_OF_2026_000043, NOM_OF_2026_000043, TypeDElementEngage.PRODUIT);
    var suivi = SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(element)
      .engagement(engagementParLeroy())
      .journal(JournalDAtelier.vide())
      .enregistre(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> {
      entities
        .createNativeQuery(
          "insert into element_de_fabrication (id, type, nom, reference, date_de_creation, date_de_modification) values (:id, 'PRODUIT', 'PRD-2026-000043', 'M-43', :date, :date)"
        )
        .setParameter("id", element.id().uuid())
        .setParameter("date", LE_10_MAI_2026_A_7H)
        .executeUpdate();
      entities
        .createNativeQuery("insert into poste_de_travail (id, libelle, nature) values (:id, 'Fraiseuse 1', 'Tournage')")
        .setParameter("id", POSTE_ID_FRAISEUSE_1.uuid())
        .executeUpdate();
      suivis.create(suivi);
    });

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.activites[0].categorie").value("NON_CONFORMITE"))
      .andExpect(jsonPath("$.activites[0].element.type").value("PRODUIT"))
      .andExpect(jsonPath("$.activites[0].element.nom").value(NOM_OF_2026_000043.value()))
      .andExpect(jsonPath("$.activites[0].element.reference").value("M-43"))
      .andExpect(jsonPath("$.activites[0].poste.id").value(POSTE_ID_FRAISEUSE_1.uuid().toString()))
      .andExpect(jsonPath("$.activites[0].poste.libelle").value("Fraiseuse 1"))
      .andExpect(jsonPath("$.activites[0].poste.nature").value(NATURE_FRAISAGE.value()));
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldRetainTheAutomaticEndAtTheInclusiveDeadline() throws Exception {
    var suivi = suiviDAtelierEngage().enregistre(debutSansPosteParDupontA(LE_10_MAI_2026_A_8H));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T21:00:00Z"));
    transactions.executeWithoutResult(status -> suivis.create(suivi));

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.activites[0].etat").value("TERMINEE_AUTOMATIQUEMENT"))
      .andExpect(jsonPath("$.activites[0].finRetenue").value("2026-05-10T21:00:00Z"));
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldDeriveDistinctTradesFromEachOperatorsAuthorizedPosts() throws Exception {
    when(clock.now()).thenReturn(Instant.parse("2026-09-13T10:00:00Z"));
    transactions.executeWithoutResult(status -> {
      entities
        .createNativeQuery(
          "insert into operateur (id, nom, prenom) values ('00000000-0000-0000-0000-000000000001', 'Leroy', 'Camille'), ('00000000-0000-0000-0000-000000000002', 'Martin', 'Noa')"
        )
        .executeUpdate();
      entities
        .createNativeQuery(
          "insert into poste_de_travail (id, libelle, nature) values ('00000000-0000-0000-0000-000000000001', 'Tour 1', 'Tournage'), ('00000000-0000-0000-0000-000000000002', 'Tour 2', 'Tournage'), ('00000000-0000-0000-0000-000000000003', 'Fraiseuse', 'Fraisage')"
        )
        .executeUpdate();
      entities
        .createNativeQuery(
          "insert into operateur_poste (operateur_id, poste_id) select '00000000-0000-0000-0000-000000000001'::uuid, id from poste_de_travail"
        )
        .executeUpdate();
    });

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.operateurs.length()").value(2))
      .andExpect(jsonPath("$.operateurs[0].metiers.length()").value(2))
      .andExpect(jsonPath("$.operateurs[0].metiers[0]").value("Fraisage"))
      .andExpect(jsonPath("$.operateurs[0].metiers[1]").value("Tournage"))
      .andExpect(jsonPath("$.operateurs[1].metiers").isEmpty());
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

package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
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
    java.util.List.of("supervision_fixture", "supervision_voisine").forEach(tenant -> {
      TenantSecurityContexts.authenticateOn(tenant);
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
  void shouldKeepConflictDescriptionsSeparateFromInterpretableActivitiesAfterTheirDeadline() throws Exception {
    var travail = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var nc = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nc).enregistre(finDe(travail).a(LE_10_MAI_2026_A_17H));
    when(clock.now()).thenReturn(LE_11_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> suivis.create(suivi));

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites").isEmpty())
      .andExpect(jsonPath("$.sequencesEnConflit.length()").value(1))
      .andExpect(jsonPath("$.sequencesEnConflit[0].id").value(travail.id().uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].operateurId").value(OPERATEUR_ID_DUPONT.uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].poste").doesNotExist())
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites.length()").value(2))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].id").value(travail.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].operateurId").value(OPERATEUR_ID_DUPONT.uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].element.nom").value(NOM_OF_2026_000042.value()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].element.type").value("ORDRE_DE_FABRICATION"))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].categorie").value("TRAVAIL"))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].debut").value("2026-05-10T08:00:00Z"))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].echeance").value("2026-05-10T21:00:00Z"))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].etat").doesNotExist())
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[0].finRetenue").doesNotExist())
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[1].id").value(nc.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites[1].categorie").value("NON_CONFORMITE"));
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldKeepAnAutomaticEndAfterRelaunchAndClosingThenRemoveItAfterRegularization() throws Exception {
    var ancien = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var reprise = debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(ancien).enregistre(reprise).cloture(clotureParLeroyA(LE_11_MAI_2026_A_9H));
    when(clock.now()).thenReturn(LE_11_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> {
      entities
        .createNativeQuery("insert into poste_de_travail (id, libelle, nature) values (:id, 'Fraiseuse 1', 'fraisage')")
        .setParameter("id", POSTE_ID_FRAISEUSE_1.uuid())
        .executeUpdate();
      suivis.create(suivi);
    });

    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.activites[0].id").value(ancien.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.activites[0].finRetenue").value("2026-05-10T21:00:00Z"));

    TenantSecurityContexts.authenticateOn("supervision_fixture");
    var regularise = suivi.enregistre(finRegulariseeParLeroyDe(ancien).a(LE_10_MAI_2026_A_17H));
    transactions.executeWithoutResult(status -> suivis.update(regularise));
    rest.perform(get("/api/atelier/supervision")).andExpect(status().isOk()).andExpect(jsonPath("$.activites").isEmpty());
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldKeepTheOriginalIdentityWhenAStartCorrectionRemovesTheAutomaticEnd() throws Exception {
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(debut);
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T22:00:00Z"));
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites[0].etat").value("TERMINEE_AUTOMATIQUEMENT"));

    TenantSecurityContexts.authenticateOn("supervision_fixture");
    var corrige = suivi.corrige(debut.id(), annulationParLeroy(), debutSansPosteParDupontA(LE_10_MAI_2026_A_12H));
    transactions.executeWithoutResult(status -> suivis.update(corrige));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T22:00:00Z"));
    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.activites[0].id").value(debut.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.activites[0].debut").value("2026-05-10T12:00:00Z"))
      .andExpect(jsonPath("$.activites[0].echeance").value("2026-05-11T01:00:00Z"))
      .andExpect(jsonPath("$.activites[0].etat").value("EN_COURS"))
      .andExpect(jsonPath("$.activites[0].finRetenue").doesNotExist());
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldKeepAnEmptyConflictAlongsideIndependentWorkAndRemoveItAfterResolution() throws Exception {
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(debut).a(LE_10_MAI_2026_A_9H);
    var reprise = debutSansPosteParDupontA(LE_11_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(debut).enregistre(fin).annule(debut.id(), annulationParLeroy()).enregistre(reprise);
    when(clock.now()).thenReturn(LE_11_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> suivis.create(suivi));
    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.activites[0].id").value(reprise.activite().orElseThrow().uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit.length()").value(1))
      .andExpect(jsonPath("$.sequencesEnConflit[0].id").value(fin.id().uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].operateurId").value(OPERATEUR_ID_DUPONT.uuid().toString()))
      .andExpect(jsonPath("$.sequencesEnConflit[0].activites").isEmpty());

    TenantSecurityContexts.authenticateOn("supervision_fixture");
    transactions.executeWithoutResult(status -> suivis.update(suivi.annule(fin.id(), annulationParLeroy())));
    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.sequencesEnConflit").isEmpty());
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldRereadCurrentOperatorTradesElementReferenceAndPostLabel() throws Exception {
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> {
      entities
        .createNativeQuery("insert into operateur (id, nom, prenom) values (:id, 'Dupont', 'Jean')")
        .setParameter("id", OPERATEUR_ID_DUPONT.uuid())
        .executeUpdate();
      entities
        .createNativeQuery("insert into poste_de_travail (id, libelle, nature) values (:id, 'Fraiseuse 1', 'fraisage')")
        .setParameter("id", POSTE_ID_FRAISEUSE_1.uuid())
        .executeUpdate();
      entities
        .createNativeQuery("insert into operateur_poste (operateur_id,poste_id) values (:operateur,:poste)")
        .setParameter("operateur", OPERATEUR_ID_DUPONT.uuid())
        .setParameter("poste", POSTE_ID_FRAISEUSE_1.uuid())
        .executeUpdate();
      entities
        .createNativeQuery(
          "insert into element_de_fabrication (id,type,nom,reference,date_de_creation,date_de_modification) values (:id,'ORDRE_DE_FABRICATION','OF-2026-000042','R-42',:date,:date)"
        )
        .setParameter("id", ELEMENT_OF_2026_000042.uuid())
        .setParameter("date", LE_10_MAI_2026_A_7H)
        .executeUpdate();
      suivis.create(suivi);
    });
    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.operateurs[0].nom").value("Dupont"))
      .andExpect(jsonPath("$.operateurs[0].metiers[0]").value("fraisage"))
      .andExpect(jsonPath("$.activites[0].element.reference").value("R-42"));

    TenantSecurityContexts.authenticateOn("supervision_fixture");
    transactions.executeWithoutResult(status -> {
      entities.createNativeQuery("update operateur set nom='Durand', prenom='Camille'").executeUpdate();
      entities.createNativeQuery("update poste_de_travail set libelle='Centre 1', nature='tournage'").executeUpdate();
      entities.createNativeQuery("update element_de_fabrication set reference='R-43', nom='OF-renomme'").executeUpdate();
    });
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_12H);
    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.evaluation").value("2026-05-10T12:00:00Z"))
      .andExpect(jsonPath("$.operateurs[0].nom").value("Durand"))
      .andExpect(jsonPath("$.operateurs[0].prenom").value("Camille"))
      .andExpect(jsonPath("$.operateurs[0].metiers[0]").value("tournage"))
      .andExpect(jsonPath("$.activites[0].element.reference").value("R-43"))
      .andExpect(jsonPath("$.activites[0].element.nom").value(NOM_OF_2026_000042.value()))
      .andExpect(jsonPath("$.activites[0].poste.libelle").value("Centre 1"))
      .andExpect(jsonPath("$.activites[0].poste.nature").value("fraisage"));
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldReadEveryCollectionFromOnlyTheAuthenticatedCompany() throws Exception {
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(debut);
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> {
      entities
        .createNativeQuery("insert into operateur (id,nom,prenom) values (:id,'Dupont','Jean')")
        .setParameter("id", OPERATEUR_ID_DUPONT.uuid())
        .executeUpdate();
      suivis.create(suivi);
    });
    TenantSecurityContexts.authenticateOn("supervision_voisine");
    var autre = suiviDAtelierEngage()
      .enregistre(debut)
      .enregistre(finDe(debut).a(LE_10_MAI_2026_A_9H))
      .annule(debut.id(), annulationParLeroy());
    transactions.executeWithoutResult(status -> {
      entities
        .createNativeQuery("insert into operateur (id,nom,prenom) values (:id,'Martin','Paul')")
        .setParameter("id", OPERATEUR_ID_DUPONT.uuid())
        .executeUpdate();
      suivis.create(autre);
    });
    rest
      .perform(get("/api/atelier/supervision").with(readerFrom("supervision_voisine")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.operateurs.length()").value(1))
      .andExpect(jsonPath("$.operateurs[0].nom").value("Martin"))
      .andExpect(jsonPath("$.activites").isEmpty())
      .andExpect(jsonPath("$.sequencesEnConflit.length()").value(1));

    TenantSecurityContexts.authenticateOn("supervision_fixture");
    rest
      .perform(get("/api/atelier/supervision").with(readerFrom("supervision_fixture")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.operateurs.length()").value(1))
      .andExpect(jsonPath("$.operateurs[0].nom").value("Dupont"))
      .andExpect(jsonPath("$.activites.length()").value(1))
      .andExpect(jsonPath("$.sequencesEnConflit").isEmpty());
  }

  @ParameterizedTest
  @CsvSource({ "ROLE_USER,200", "ROLE_GESTIONNAIRE,200", "ROLE_ADMIN,403", "ROLE_AUTRE,403" })
  void shouldRestrictSupervisionToBusinessReaders(String role, int statut) throws Exception {
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_9H);
    rest
      .perform(
        get("/api/atelier/supervision").with(
          jwt()
            .jwt(token -> token.claim("tenant", "supervision_fixture"))
            .authorities(new SimpleGrantedAuthority(role))
        )
      )
      .andExpect(status().is(statut));
  }

  @Test
  void shouldRejectAnUnknownCompanyBeforeReadingSupervision() throws Exception {
    rest
      .perform(
        get("/api/atelier/supervision").with(
          jwt()
            .jwt(token -> token.claim("tenant", "inconnue"))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
        )
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectAuthenticationWithoutACompany() throws Exception {
    rest
      .perform(get("/api/atelier/supervision").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
      .andExpect(status().isForbidden());
  }

  @Test
  void shouldRequireAuthenticationForSupervision() throws Exception {
    rest.perform(get("/api/atelier/supervision").with(anonymous())).andExpect(status().isUnauthorized());
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldReturnTheCompleteOperatorDirectoryBeyondAUsualPage() throws Exception {
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_9H);
    transactions.executeWithoutResult(status ->
      entities
        .createNativeQuery(
          "insert into operateur (id,nom,prenom) select gen_random_uuid(), 'Operateur ' || n, 'Camille' from generate_series(1,121) n"
        )
        .executeUpdate()
    );
    rest.perform(get("/api/atelier/supervision")).andExpect(status().isOk()).andExpect(jsonPath("$.operateurs.length()").value(121));
  }

  @Test
  @WithTenant("supervision_fixture")
  void shouldKeepAPostWithoutInventingAnAbsentProjectedNature() throws Exception {
    var suivi = suiviDAtelierEngage().enregistre(debutSansNatureSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    when(clock.now()).thenReturn(LE_10_MAI_2026_A_9H);
    transactions.executeWithoutResult(status -> {
      entities
        .createNativeQuery("insert into poste_de_travail (id,libelle,nature) values (:id,'Fraiseuse 1','fraisage')")
        .setParameter("id", POSTE_ID_FRAISEUSE_1.uuid())
        .executeUpdate();
      suivis.create(suivi);
    });
    rest
      .perform(get("/api/atelier/supervision"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activites[0].poste.id").value(POSTE_ID_FRAISEUSE_1.uuid().toString()))
      .andExpect(jsonPath("$.activites[0].poste.libelle").value("Fraiseuse 1"))
      .andExpect(jsonPath("$.activites[0].poste.nature").doesNotExist());
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

  private static JwtRequestPostProcessor readerFrom(String tenant) {
    return jwt()
      .jwt(token -> token.claim("tenant", tenant))
      .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }
}

package com.glm.glmback.wire.openapi.infrastructure.primary;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class OpenApiConfigurationIT {

  @Autowired
  private MockMvc rest;

  @Test
  void shouldServirLaDescriptionSansJeton() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.info.title").value("glmproject API"))
      .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"));
  }

  @Test
  void shouldDecrireLesActivitesSansLesSurfacesRetirees() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis'].post.tags[0]").value("Atelier - elements engages"))
      .andExpect(jsonPath("$.paths['/api/atelier/journees']").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/journees/pointages']").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestJourneeDeTravail").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestEvenementDePresence").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestAnomalieDePresence").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/parametrage/amplitude-maximale']").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestAmplitudeMaximale").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/temps-effectif']").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/cloture'].delete.summary").exists());
  }

  @Test
  void shouldPublierLesAnomaliesDePointageSousLeurTagEtSansLesAnciennesRoutesDeConflits() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.paths['/api/atelier/anomalies'].get.tags[0]").value("Atelier - anomalies de pointage"))
      .andExpect(jsonPath("$.paths['/api/atelier/anomalies'].get.parameters[?(@.name == 'nature')]").isEmpty())
      .andExpect(
        jsonPath("$.paths['/api/atelier/anomalies'].get.responses['200'].content['*/*'].schema['$ref']").value(
          "#/components/schemas/PageRestFinAutomatiqueEnListe"
        )
      )
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/anomalies/{pointage}'].get.tags[0]").value("Atelier - anomalies de pointage"))
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/anomalies/{pointage}/apercus']").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{suivi}/confirmations-de-resolution']").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{suivi}/confirmations-de-resolution/{commande}']").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/conflits']").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/conflits/{pointage}']").doesNotExist())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/conflits/{pointage}/apercus']").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestDossierAnomalie").exists())
      .andExpect(jsonPath("$.components.schemas.RestAdresseDossierAnomalie").exists())
      .andExpect(jsonPath("$.components.schemas.PageRestFinAutomatiqueEnListe").exists())
      .andExpect(jsonPath("$.components.schemas.RestFinAutomatiqueEnListe").exists())
      .andExpect(jsonPath("$.components.schemas.RestConflitEnListe").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestAnomalieEnListe").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestPageDesAnomalies").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestDossierConflit").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestAdresseDossierConflit").doesNotExist())
      .andExpect(jsonPath("$.components.schemas.RestPageDesConflits").doesNotExist());
  }

  @Test
  void shouldDecrireHonnetementLesProprietesGarantiesDesReponses() throws Exception {
    ResultActions specification = rest.perform(get("/v3/api-docs")).andExpect(status().isOk());

    specification
      .andExpect(
        requiredFields(
          "RestSuiviDAtelier",
          "id",
          "element",
          "nom",
          "categorie",
          "engagePar",
          "engageLe",
          "etat",
          "journal",
          "activitesEnCours"
        )
      )
      .andExpect(
        requiredFields(
          "RestSuiviDAtelierEnGrille",
          "id",
          "element",
          "nom",
          "categorie",
          "engagePar",
          "engageLe",
          "etat",
          "activitesEnCours"
        )
      )
      .andExpect(requiredFields("RestActiviteEnCours", "categorie", "depuis", "ouverture", "echeance"))
      .andExpect(
        requiredFields(
          "RestEvenementDAtelier",
          "id",
          "type",
          "operateurId",
          "auteur",
          "dateDeSurvenue",
          "dateDEnregistrement",
          "estUneRegularisation"
        )
      )
      .andExpect(requiredFields("RestOperateur", "id", "nom", "prenom", "postes", "natures"))
      .andExpect(requiredFields("RestOperateurDAtelier", "id", "nom", "prenom"))
      .andExpect(requiredFields("RestOperateurDeFeuilleDeTemps", "id", "nom", "prenom"))
      .andExpect(requiredFields("RestPosteDAtelier", "id", "libelle"))
      .andExpect(requiredFields("RestPosteHabilite", "id", "libelle", "nature"))
      .andExpect(requiredFields("PageRestElementDeFabrication", "content", "currentPage", "pageSize", "totalElementsCount"))
      .andExpect(requiredFields("PageRestOperateur", "content", "currentPage", "pageSize", "totalElementsCount"))
      .andExpect(requiredFields("PageRestPosteDeTravail", "content", "currentPage", "pageSize", "totalElementsCount"))
      .andExpect(requiredFields("PageRestSuiviDAtelierEnGrille", "content", "currentPage", "pageSize", "totalElementsCount"));
  }

  @Test
  void shouldDistinguerLesProjectionsHomonymesDansLesReponses() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(reference("RestActiviteEnCours", "operateur", "RestOperateurDAtelier"))
      .andExpect(reference("RestActiviteEnCours", "poste", "RestPosteDAtelier"))
      .andExpect(reference("RestEvenementDAtelier", "operateur", "RestOperateurDAtelier"))
      .andExpect(reference("RestEvenementDAtelier", "poste", "RestPosteDAtelier"))
      .andExpect(reference("RestFeuilleDeTemps", "operateur", "RestOperateurDeFeuilleDeTemps"))
      .andExpect(reference("PageRestOperateur", "content.items", "RestOperateur"))
      .andExpect(reference("PageRestPosteDeTravail", "content.items", "RestPosteDeTravail"));
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNePlusRouterLesJournees() throws Exception {
    rest.perform(get("/api/atelier/journees")).andExpect(status().isNotFound());
    rest.perform(post("/api/atelier/journees/pointages").contentType("application/json").content("{}")).andExpect(status().isNotFound());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNePlusRouterLeParametrageDAmplitude() throws Exception {
    rest.perform(put("/api/parametrage/amplitude-maximale").contentType("application/json").content("{}")).andExpect(status().isNotFound());
  }

  private static org.springframework.test.web.servlet.ResultMatcher requiredFields(String schema, String... fields) {
    return jsonPath("$.components.schemas." + schema + ".required").value(containsInAnyOrder(fields));
  }

  private static org.springframework.test.web.servlet.ResultMatcher reference(String schema, String property, String targetSchema) {
    return jsonPath("$.components.schemas." + schema + ".properties." + property + "['$ref']").value(
      "#/components/schemas/" + targetSchema
    );
  }
}

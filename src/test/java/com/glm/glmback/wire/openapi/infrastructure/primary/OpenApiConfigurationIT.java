package com.glm.glmback.wire.openapi.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.glm.glmback.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
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
  void shouldDecrireLesDeuxSurfacesDeLAtelier() throws Exception {
    rest
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis'].post.tags[0]").value("Atelier - elements engages"))
      .andExpect(jsonPath("$.paths['/api/atelier/journees/pointages'].post.tags[0]").value("Atelier - presence des operateurs"))
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/temps-effectif'].get.summary").exists())
      .andExpect(jsonPath("$.paths['/api/atelier/suivis/{id}/cloture'].delete.summary").exists());
  }

  @Test
  void shouldDecrireHonnetementLesProprietesGarantiesDesReponses() throws Exception {
    ResultActions specification = rest.perform(get("/v3/api-docs")).andExpect(status().isOk());

    specification
      .andExpect(
        requiredFields("RestSuiviDAtelier", "id", "element", "nom", "type", "engagePar", "engageLe", "etat", "journal", "activitesEnCours")
      )
      .andExpect(
        requiredFields("RestSuiviDAtelierEnGrille", "id", "element", "nom", "type", "engagePar", "engageLe", "etat", "activitesEnCours")
      )
      .andExpect(requiredFields("RestActiviteEnCours", "categorie", "depuis"))
      .andExpect(
        requiredFields("RestEvenementDAtelier", "id", "type", "auteur", "dateDeSurvenue", "dateDEnregistrement", "estUneRegularisation")
      )
      .andExpect(requiredFields("RestOperateur", "id", "nom", "prenom", "postes", "natures"))
      .andExpect(requiredFields("RestOperateurDAtelier", "id", "nom", "prenom"))
      .andExpect(requiredFields("RestOperateurDeFeuilleDeTemps", "id", "nom", "prenom"))
      .andExpect(requiredFields("RestPosteDAtelier", "id", "libelle"))
      .andExpect(requiredFields("RestPosteHabilite", "id", "libelle", "nature"))
      .andExpect(requiredFields("PageRestElementDeFabrication", "content", "currentPage", "pageSize", "totalElementsCount"))
      .andExpect(requiredFields("PageRestJourneeDeTravail", "content", "currentPage", "pageSize", "totalElementsCount"))
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
  @DisabledIfSystemProperty(
    named = OpenApiSpecification.REGENERATION_FLAG,
    matches = "true",
    disabledReason = "this run is rewriting the committed file, so the comparison would judge the version being replaced"
  )
  void shouldMatchTheCommittedSpecification() throws Exception {
    assertThat(OpenApiSpecification.served(rest))
      .describedAs(OpenApiSpecification.outOfDateMessage())
      .isEqualTo(OpenApiSpecification.committed());
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

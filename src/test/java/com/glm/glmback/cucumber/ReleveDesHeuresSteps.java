package com.glm.glmback.cucumber;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/** Compose les deux lectures REST du releve avec un meme instant explicite. */
public class ReleveDesHeuresSteps {

  @Autowired
  private CucumberRestClient rest;

  private final Map<String, Boolean> completudeParJour = new HashMap<>();

  @SuppressWarnings("unchecked")
  @When("je lis la synthese du releve avec l'instant rendu par la feuille")
  public void litLaSyntheseAuMemeInstant() {
    completudeParJour.clear();
    List<Map<String, Object>> jours = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours");
    for (Map<String, Object> jour : jours) {
      List<Map<String, Object>> activites = (List<Map<String, Object>>) jour.get("activites");
      completudeParJour.put(
        (String) jour.get("jour"),
        activites.stream().noneMatch(activite -> "A_RESOUDRE".equals(((Map<?, ?>) activite.get("activite")).get("etat")))
      );
    }
    rest.get(
      "/api/syntheses-des-heures/{operateur}?annee={annee}&semaine={semaine}&evaluation={evaluation}",
      Map.of(
        "operateur",
        CucumberRestTestContext.getElement("$.operateur.id"),
        "annee",
        CucumberRestTestContext.getElement("$.annee"),
        "semaine",
        CucumberRestTestContext.getElement("$.semaine"),
        "evaluation",
        CucumberRestTestContext.getElement("$.evaluation")
      )
    );
  }

  @Then("la synthese du releve compte {string} a l'instant {string}")
  public void compteA(String duree, String evaluation) {
    assertThat(CucumberRestTestContext.getElement("$.dureeOperationnelleTotale")).isEqualTo(Map.of("complete", true, "valeur", duree));
    assertThat(CucumberRestTestContext.getElement("$.evaluation")).isEqualTo(evaluation);
  }

  @Then("la synthese du releve conserve les jours a resoudre de la feuille")
  @SuppressWarnings("unchecked")
  public void gardeLesJoursAResoudre() {
    List<Map<String, Object>> jours = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours");
    assertThat(jours).hasSize(completudeParJour.size());
    for (Map<String, Object> jour : jours) {
      Map<String, Object> duree = (Map<String, Object>) jour.get("dureeOperationnelle");
      boolean complete = completudeParJour.get((String) jour.get("jour"));
      assertThat(duree.get("complete")).isEqualTo(complete);
      if (!complete) {
        assertThat(duree).containsOnlyKeys("complete");
      }
    }
  }
}

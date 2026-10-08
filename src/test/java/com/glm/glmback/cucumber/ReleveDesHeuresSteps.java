package com.glm.glmback.cucumber;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/** Compose les deux lectures REST du releve avec un meme instant explicite. */
public class ReleveDesHeuresSteps {

  @Autowired
  private CucumberRestClient rest;

  @When("je lis la synthese du releve avec l'instant rendu par la feuille")
  public void litLaSyntheseAuMemeInstant() {
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
}

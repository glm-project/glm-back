package com.glm.glmback.parametrage.cucumber;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class ParametrageSteps {

  private static final String BASE_URI = "/api/parametrage";

  @Autowired
  private CucumberRestClient rest;

  @When("je lis le parametrage de l'entreprise")
  public void jeLisLeParametrageDeLEntreprise() {
    rest.get(BASE_URI);
  }

  @Then("la duree max d'une activite vaut {string}")
  public void laDureeMaxDUneActiviteVaut(String duree) {
    assertThat(CucumberRestTestContext.getElement("$.dureeMaxDActivite")).isEqualTo(duree);
  }
}

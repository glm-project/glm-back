package com.glm.glmback.parametrage.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ParametrageSteps {

  private static final String BASE_URI = "/api/parametrage";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  @When("je lis le parametrage de l'entreprise")
  public void jeLisLeParametrageDeLEntreprise() {
    rest.get(BASE_URI);
  }

  @When("je fixe la duree max d'une activite a {string}")
  public void jeFixeLaDureeMaxDUneActiviteA(String duree) {
    rest.put(BASE_URI + "/duree-max-d-activite", JSON.writeValueAsString(Map.of("dureeMaxDActivite", duree)));
  }

  @Given("j'ai fixe la duree max d'une activite a {string}")
  public void jaiFixeLaDureeMaxDUneActiviteA(String duree) {
    jeFixeLaDureeMaxDUneActiviteA(duree);
    assertThatLastResponse().hasHttpStatus(200);
  }

  @Then("la duree max d'une activite vaut {string}")
  public void laDureeMaxDUneActiviteVaut(String duree) {
    assertThat(CucumberRestTestContext.getElement("$.dureeMaxDActivite")).isEqualTo(duree);
  }
}

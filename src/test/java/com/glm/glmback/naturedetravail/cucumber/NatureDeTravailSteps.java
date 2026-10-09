package com.glm.glmback.naturedetravail.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class NatureDeTravailSteps {

  private static final String BASE_URI = "/api/natures-de-travail";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  @When("je declare la nature de travail {string}")
  public void jeDeclareLaNatureDeTravail(String libelle) {
    rest.post(BASE_URI, JSON.writeValueAsString(Map.of("libelle", libelle)));
  }

  @Given("j'ai declare la nature de travail {string}")
  public void jaiDeclareLaNatureDeTravail(String libelle) {
    jeDeclareLaNatureDeTravail(libelle);
    assertThatLastResponse().hasHttpStatus(201);
  }

  @When("je liste les natures de travail")
  public void jeListeLesNaturesDeTravail() {
    rest.get(BASE_URI + "?size=100");
  }

  @Then("la reponse de nature de travail contient")
  public void laReponseDeNatureDeTravailContient(Map<String, Object> attendu) {
    assertThatLastResponse().hasResponse().containing(attendu);
  }

  @Then("la nature de travail declaree est libre")
  public void laNatureDeTravailDeclareeEstLibre() {
    assertThat(CucumberRestTestContext.getElement("$.utilisee")).isEqualTo(false);
  }

  @Then("les natures de travail listees comprennent dans l'ordre {string}")
  public void lesNaturesDeTravailListeesComprennentDansLOrdre(String libelles) {
    List<String> attendus = List.of(libelles.split("\\s*,\\s*"));

    assertThat(libelles().stream().filter(attendus::contains).toList()).containsExactlyElementsOf(attendus);
  }

  @SuppressWarnings("unchecked")
  private static List<String> libelles() {
    return (List<String>) CucumberRestTestContext.getElement("$.content..libelle");
  }
}

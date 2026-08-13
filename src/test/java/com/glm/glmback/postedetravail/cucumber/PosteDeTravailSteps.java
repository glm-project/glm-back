package com.glm.glmback.postedetravail.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class PosteDeTravailSteps {

  private static final String BASE_URI = "/api/postes-de-travail";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  private UUID dernierIdDeclare;

  @When("je declare un poste de travail")
  public void jeDeclareUnPosteDeTravail(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(donnees));
  }

  @Given("j'ai declare un poste de travail")
  public void jaiDeclareUnPosteDeTravail(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(donnees));
    dernierIdDeclare = idDeLaDerniereReponse();
  }

  @When("je consulte ce poste de travail")
  public void jeConsulteCePosteDeTravail() {
    rest.get(BASE_URI + "/" + dernierIdDeclare);
  }

  @When("je consulte le poste de travail {string}")
  public void jeConsulteLePosteDeTravail(String id) {
    rest.get(BASE_URI + "/" + id);
  }

  @When("je revise ce poste de travail")
  public void jeReviseCePosteDeTravail(Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + dernierIdDeclare, JSON.writeValueAsString(donnees));
  }

  @When("je revise le poste de travail {string}")
  public void jeReviseLePosteDeTravail(String id, Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + id, JSON.writeValueAsString(donnees));
  }

  @When("je supprime ce poste de travail")
  public void jeSupprimeCePosteDeTravail() {
    rest.delete(BASE_URI + "/" + dernierIdDeclare);
  }

  @When("je supprime le poste de travail {string}")
  public void jeSupprimeLePosteDeTravail(String id) {
    rest.delete(BASE_URI + "/" + id);
  }

  @When("je liste les postes de travail")
  public void jeListeLesPostesDeTravail() {
    rest.get(BASE_URI);
  }

  @When("je liste les postes de travail de nature {string}")
  public void jeListeLesPostesDeTravailDeNature(String nature) {
    rest.get(BASE_URI + "?nature=" + nature);
  }

  @Then("la reponse de poste de travail contient")
  public void laReponseDePosteDeTravailContient(Map<String, Object> attendu) {
    assertThatLastResponse().hasResponse().containing(attendu);
  }

  @Then("la reponse contient au moins {int} postes de travail")
  public void laReponseContientAuMoinsPostes(int count) {
    assertThatLastResponse().hasElement("$.content").withMoreThanElementsCount(count);
  }

  @Then("la reponse ne contient que des postes de travail de nature {string}")
  public void laReponseNeContientQueDesPostesDeNature(String nature) {
    assertThat(textes("$.content..nature")).isNotEmpty().containsOnly(nature);
  }

  private UUID idDeLaDerniereReponse() {
    return UUID.fromString((String) CucumberRestTestContext.getElement("$.id"));
  }

  @SuppressWarnings("unchecked")
  private static List<String> textes(String chemin) {
    return (List<String>) CucumberRestTestContext.getElement(chemin);
  }
}

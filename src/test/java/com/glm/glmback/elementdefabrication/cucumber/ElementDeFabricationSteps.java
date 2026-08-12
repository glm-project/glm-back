package com.glm.glmback.elementdefabrication.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ElementDeFabricationSteps {

  private static final String BASE_URI = "/api/elements-de-fabrication";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  private UUID dernierIdCree;

  @When("je cree un element de fabrication")
  public void jeCreeUnElementDeFabrication(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(donnees));
  }

  @Given("j'ai cree un element de fabrication")
  public void jaiCreeUnElementDeFabrication(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(donnees));
    dernierIdCree = idDeLaDerniereReponse();
  }

  @When("je consulte cet element de fabrication")
  public void jeConsulteCetElementDeFabrication() {
    rest.get(BASE_URI + "/" + dernierIdCree);
  }

  @When("je consulte l'element de fabrication {string}")
  public void jeConsulteLElementDeFabrication(String id) {
    rest.get(BASE_URI + "/" + id);
  }

  @When("je modifie cet element de fabrication")
  public void jeModifieCetElementDeFabrication(Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + dernierIdCree, JSON.writeValueAsString(donnees));
  }

  @When("je modifie cet element de fabrication sans reference ni description")
  public void jeModifieCetElementDeFabricationSansReferenceNiDescription() {
    rest.put(BASE_URI + "/" + dernierIdCree, "{}");
  }

  @When("je modifie l'element de fabrication {string}")
  public void jeModifieLElementDeFabrication(String id, Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + id, JSON.writeValueAsString(donnees));
  }

  @When("je supprime cet element de fabrication")
  public void jeSupprimeCetElementDeFabrication() {
    rest.delete(BASE_URI + "/" + dernierIdCree);
  }

  @When("je supprime l'element de fabrication {string}")
  public void jeSupprimeLElementDeFabrication(String id) {
    rest.delete(BASE_URI + "/" + id);
  }

  @When("je liste les elements de fabrication entre {string} et {string}")
  public void jeListeLesElementsDeFabrication(String debut, String fin) {
    rest.get(BASE_URI + "?debut=" + debut + "&fin=" + fin);
  }

  @Then("la reponse d'element de fabrication contient")
  public void laReponseDElementDeFabricationContient(Map<String, Object> attendu) {
    assertThatLastResponse().hasResponse().containing(attendu);
  }

  @Then("la reponse d'element de fabrication a un nom commencant par {string}")
  public void laReponseAUnNomCommencantPar(String prefixe) {
    assertThat((String) CucumberRestTestContext.getElement("$.nom")).startsWith(prefixe);
  }

  @Then("la reponse d'element de fabrication n'a ni reference ni description")
  public void laReponseDElementDeFabricationNAPasDeReferenceNiDescription() {
    assertThat(CucumberRestTestContext.getElement("$.reference")).isNull();
    assertThat(CucumberRestTestContext.getElement("$.description")).isNull();
  }

  @Then("la reponse contient au moins {int} elements")
  public void laReponseContientAuMoins(int count) {
    assertThatLastResponse().hasElement("$.content").withMoreThanElementsCount(count);
  }

  private UUID idDeLaDerniereReponse() {
    return UUID.fromString((String) CucumberRestTestContext.getElement("$.id"));
  }
}

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

public class ElementDeFabricationSteps {

  private static final String BASE_URI = "/api/elements-de-fabrication";
  private static final String CORPS_ORDRE_PAR_DEFAUT = "{\"titre\": \"Assemblage carter\", \"description\": \"Carter en fonte\"}";
  private static final String CORPS_PRODUIT_PAR_DEFAUT = "{\"titre\": \"Carter moteur\", \"description\": \"Carter en fonte\"}";

  @Autowired
  private CucumberRestClient rest;

  private UUID dernierIdCree;

  @When("je cree un ordre de fabrication avec le corps")
  public void jeCreeUnOrdreDeFabrication(String corps) {
    rest.post(BASE_URI + "/ordres-de-fabrication", corps);
  }

  @When("je cree un produit avec le corps")
  public void jeCreeUnProduit(String corps) {
    rest.post(BASE_URI + "/produits", corps);
  }

  @Given("j'ai cree un ordre de fabrication")
  public void jaiCreeUnOrdreDeFabrication() {
    rest.post(BASE_URI + "/ordres-de-fabrication", CORPS_ORDRE_PAR_DEFAUT);
    dernierIdCree = idDeLaDerniereReponse();
  }

  @Given("j'ai cree un produit")
  public void jaiCreeUnProduit() {
    rest.post(BASE_URI + "/produits", CORPS_PRODUIT_PAR_DEFAUT);
    dernierIdCree = idDeLaDerniereReponse();
  }

  @When("je consulte cet ordre de fabrication")
  public void jeConsulteCetOrdreDeFabrication() {
    rest.get(BASE_URI + "/ordres-de-fabrication/" + dernierIdCree);
  }

  @When("je consulte l'ordre de fabrication {string}")
  public void jeConsulteLOrdreDeFabrication(String id) {
    rest.get(BASE_URI + "/ordres-de-fabrication/" + id);
  }

  @When("je consulte ce produit")
  public void jeConsulteCeProduit() {
    rest.get(BASE_URI + "/produits/" + dernierIdCree);
  }

  @When("je modifie cet ordre de fabrication avec le corps")
  public void jeModifieCetOrdreDeFabrication(String corps) {
    rest.put(BASE_URI + "/ordres-de-fabrication/" + dernierIdCree, corps);
  }

  @When("je modifie ce produit avec le corps")
  public void jeModifieCeProduit(String corps) {
    rest.put(BASE_URI + "/produits/" + dernierIdCree, corps);
  }

  @When("je modifie le produit {string} avec le corps")
  public void jeModifieLeProduit(String id, String corps) {
    rest.put(BASE_URI + "/produits/" + id, corps);
  }

  @When("je supprime cet ordre de fabrication")
  public void jeSupprimeCetOrdreDeFabrication() {
    rest.delete(BASE_URI + "/ordres-de-fabrication/" + dernierIdCree);
  }

  @When("je supprime ce produit")
  public void jeSupprimeCeProduit() {
    rest.delete(BASE_URI + "/produits/" + dernierIdCree);
  }

  @When("je supprime le produit {string}")
  public void jeSupprimeLeProduit(String id) {
    rest.delete(BASE_URI + "/produits/" + id);
  }

  @When("je liste les elements de fabrication entre {string} et {string}")
  public void jeListeLesElementsDeFabrication(String debut, String fin) {
    rest.get(BASE_URI + "?debut=" + debut + "&fin=" + fin);
  }

  @Then("la reponse a le statut http {int}")
  public void laReponseALeStatutHttp(int status) {
    assertThatLastResponse().hasHttpStatus(status);
  }

  @Then("la reponse d'element de fabrication contient")
  public void laReponseDElementDeFabricationContient(Map<String, Object> attendu) {
    assertThatLastResponse().hasResponse().containing(attendu);
  }

  @Then("la reponse d'element de fabrication a un nom commencant par {string}")
  public void laReponseAUnNomCommencantPar(String prefixe) {
    assertThat((String) CucumberRestTestContext.getElement("$.nom")).startsWith(prefixe);
  }

  @Then("la reponse contient au moins {int} elements")
  public void laReponseContientAuMoins(int count) {
    assertThatLastResponse().hasElement("$.content").withMoreThanElementsCount(count);
  }

  private UUID idDeLaDerniereReponse() {
    return UUID.fromString((String) CucumberRestTestContext.getElement("$.id"));
  }
}

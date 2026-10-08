package com.glm.glmback.categoriedeproduit.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class CategorieDeProduitSteps {

  private static final String BASE_URI = "/api/categories-de-produit";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  @When("je declare la categorie de produit {string}")
  public void jeDeclareLaCategorieDeProduit(String code) {
    rest.post(BASE_URI, JSON.writeValueAsString(Map.of("code", code)));
  }

  @Given("j'ai declare la categorie de produit {string}")
  public void jaiDeclareLaCategorieDeProduit(String code) {
    jeDeclareLaCategorieDeProduit(code);
    assertThatLastResponse().hasHttpStatus(201);
  }

  @When("je supprime la categorie de produit {string}")
  public void jeSupprimeLaCategorieDeProduit(String code) {
    rest.delete(BASE_URI + "/" + code);
  }

  @Given("j'ai supprime la categorie de produit {string}")
  public void jaiSupprimeLaCategorieDeProduit(String code) {
    jeSupprimeLaCategorieDeProduit(code);
    assertThatLastResponse().hasHttpStatus(204);
  }

  @When("je place la categorie de produit {string} en tete")
  public void jePlaceLaCategorieDeProduitEnTete(String code) {
    jeListeLesCategoriesDeProduit();
    List<String> ordre = new ArrayList<>(codes());
    ordre.remove(code);
    ordre.addFirst(code);
    reordonne(ordre);
  }

  @When("je reordonne les categories de produit en {string}")
  public void jeReordonneLesCategoriesDeProduitEn(String codes) {
    reordonne(liste(codes));
  }

  @When("je liste les categories de produit")
  public void jeListeLesCategoriesDeProduit() {
    rest.get(BASE_URI + "?size=100");
  }

  @Then("la reponse de categorie de produit contient")
  public void laReponseDeCategorieDeProduitContient(Map<String, Object> attendu) {
    assertThatLastResponse().hasResponse().containing(attendu);
  }

  @Then("les categories de produit se terminent par {string}")
  public void lesCategoriesDeProduitSeTerminentPar(String codes) {
    assertThat(codes()).endsWith(liste(codes).toArray(String[]::new));
  }

  @Then("les categories de produit commencent par {string}")
  public void lesCategoriesDeProduitCommencentPar(String codes) {
    jeListeLesCategoriesDeProduit();
    assertThat(codes()).startsWith(liste(codes).toArray(String[]::new));
  }

  @Then("la categorie de produit {string} n'est plus listee")
  public void laCategorieDeProduitNEstPlusListee(String code) {
    jeListeLesCategoriesDeProduit();
    assertThat(codes()).doesNotContain(code);
  }

  private void reordonne(List<String> codes) {
    rest.put(BASE_URI + "/ordre", JSON.writeValueAsString(Map.of("codes", codes)));
  }

  private static List<String> liste(String codes) {
    return Arrays.stream(codes.split(",")).map(String::trim).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<String> codes() {
    return (List<String>) CucumberRestTestContext.getElement("$.content..code");
  }
}

package com.glm.glmback.naturedetravail.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class NatureDeTravailSteps {

  private static final String BASE_URI = "/api/natures-de-travail";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  private final Map<String, String> ids = new HashMap<>();

  @When("je declare la nature de travail {string}")
  public void jeDeclareLaNatureDeTravail(String libelle) {
    rest.post(BASE_URI, JSON.writeValueAsString(Map.of("libelle", libelle)));
  }

  @Given("j'ai declare la nature de travail {string}")
  public void jaiDeclareLaNatureDeTravail(String libelle) {
    jeDeclareLaNatureDeTravail(libelle);
    assertThatLastResponse().hasHttpStatus(201);
    ids.put(libelle, (String) CucumberRestTestContext.getElement("$.id"));
  }

  @When("je renomme la nature de travail {string} en {string}")
  public void jeRenommeLaNatureDeTravailEn(String libelle, String nouveauLibelle) {
    rest.put(BASE_URI + "/" + ids.get(libelle), JSON.writeValueAsString(Map.of("libelle", nouveauLibelle)));
  }

  @When("je supprime la nature de travail {string}")
  public void jeSupprimeLaNatureDeTravail(String libelle) {
    rest.delete(BASE_URI + "/" + ids.get(libelle));
  }

  @Given("j'ai supprime la nature de travail {string}")
  public void jaiSupprimeLaNatureDeTravail(String libelle) {
    jeSupprimeLaNatureDeTravail(libelle);
    assertThatLastResponse().hasHttpStatus(204);
  }

  @Given("le poste de travail {string} porte la nature de travail {string}")
  public void lePosteDeTravailPorteLaNatureDeTravail(String poste, String libelle) {
    rest.post("/api/postes-de-travail", JSON.writeValueAsString(Map.of("libelle", poste, "natureId", ids.get(libelle))));
    assertThatLastResponse().hasHttpStatus(201);
  }

  @Then("la nature de travail {string} est listee comme utilisee")
  public void laNatureDeTravailEstListeeCommeUtilisee(String libelle) {
    jeListeLesNaturesDeTravail();
    assertThat(CucumberRestTestContext.getElement("$.content[?(@.id == '" + ids.get(libelle) + "')].utilisee")).isEqualTo(List.of(true));
  }

  @Then("la nature de travail {string} est listee avec {int} poste(s)")
  public void laNatureDeTravailEstListeeAvecPostes(String libelle, int postes) {
    jeListeLesNaturesDeTravail();
    assertThat(CucumberRestTestContext.getElement("$.content[?(@.id == '" + ids.get(libelle) + "')].postes")).isEqualTo(List.of(postes));
  }

  @Given("la nature de travail {string} est renommee en {string}")
  public void laNatureDeTravailEstRenommeeEn(String libelle, String nouveauLibelle) {
    jeListeLesNaturesDeTravail();
    @SuppressWarnings("unchecked")
    List<String> trouvees = (List<String>) CucumberRestTestContext.getElement("$.content[?(@.libelle == '" + libelle + "')].id");
    assertThat(trouvees).hasSize(1);
    rest.put(BASE_URI + "/" + trouvees.getFirst(), JSON.writeValueAsString(Map.of("libelle", nouveauLibelle)));
    assertThatLastResponse().hasHttpStatus(200);
  }

  @When("je supprime une nature de travail inconnue")
  public void jeSupprimeUneNatureDeTravailInconnue() {
    rest.delete(BASE_URI + "/" + UUID.randomUUID());
  }

  @Then("la nature de travail {string} n'est plus listee")
  public void laNatureDeTravailNEstPlusListee(String libelle) {
    jeListeLesNaturesDeTravail();
    assertThat(CucumberRestTestContext.getElement("$.content[?(@.id == '" + ids.get(libelle) + "')]")).isEqualTo(List.of());
  }

  @When("je renomme une nature de travail inconnue en {string}")
  public void jeRenommeUneNatureDeTravailInconnueEn(String nouveauLibelle) {
    rest.put(BASE_URI + "/" + UUID.randomUUID(), JSON.writeValueAsString(Map.of("libelle", nouveauLibelle)));
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

  @Then("la nature de travail {string} est listee sous le libelle {string}")
  public void laNatureDeTravailEstListeeSousLeLibelle(String libelle, String libelleListe) {
    jeListeLesNaturesDeTravail();
    assertThat(CucumberRestTestContext.getElement("$.content[?(@.id == '" + ids.get(libelle) + "')].libelle")).isEqualTo(
      List.of(libelleListe)
    );
  }

  @SuppressWarnings("unchecked")
  private static List<String> libelles() {
    return (List<String>) CucumberRestTestContext.getElement("$.content..libelle");
  }
}

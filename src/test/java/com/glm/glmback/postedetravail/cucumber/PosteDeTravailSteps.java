package com.glm.glmback.postedetravail.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.NaturesDesScenarios;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.HashMap;
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

  private final Map<String, String> naturesDeclarees = new HashMap<>();

  @When("je declare un poste de travail")
  public void jeDeclareUnPosteDeTravail(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(avecIdentifiantDeNature(donnees)));
  }

  @Given("j'ai declare un poste de travail")
  public void jaiDeclareUnPosteDeTravail(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(avecIdentifiantDeNature(donnees)));
    dernierIdDeclare = idDeLaDerniereReponse();
  }

  /**
   * Les tableaux des scenarios nomment la nature par son libelle : elle part au serveur par son identifiant.
   */
  private Map<String, String> avecIdentifiantDeNature(Map<String, String> donnees) {
    if (!donnees.containsKey("nature")) {
      return donnees;
    }
    Map<String, String> corps = new HashMap<>(donnees);
    corps.put("natureId", NaturesDesScenarios.identifiant(rest, corps.remove("nature")));
    return corps;
  }

  @When("je declare le poste de travail {string} de la nature declaree {string}")
  public void jeDeclareLePosteDeTravailDeLaNatureDeclaree(String libelle, String nature) {
    rest.post(BASE_URI, JSON.writeValueAsString(Map.of("libelle", libelle, "natureId", natureDeclaree(nature))));
  }

  @When("je declare le poste de travail {string} d'une nature inconnue")
  public void jeDeclareLePosteDeTravailDUneNatureInconnue(String libelle) {
    rest.post(BASE_URI, JSON.writeValueAsString(Map.of("libelle", libelle, "natureId", UUID.randomUUID())));
  }

  @When("je revise ce poste de travail en {string} de la nature declaree {string}")
  public void jeReviseCePosteDeTravailDeLaNatureDeclaree(String libelle, String nature) {
    rest.put(BASE_URI + "/" + dernierIdDeclare, JSON.writeValueAsString(Map.of("libelle", libelle, "natureId", natureDeclaree(nature))));
  }

  @When("je liste les postes de travail de la nature declaree {string}")
  public void jeListeLesPostesDeTravailDeLaNatureDeclaree(String nature) {
    rest.get(BASE_URI + "?size=100&natureId=" + natureDeclaree(nature));
  }

  @Then("la reponse ne contient que les postes de travail {string}")
  public void laReponseNeContientQueLesPostesDeTravail(String libelles) {
    assertThat(textes("$.content..libelle")).containsExactlyInAnyOrder(libelles.split("\\s*,\\s*"));
  }

  @Then("le poste de travail porte la nature declaree {string}")
  public void lePosteDeTravailPorteLaNatureDeclaree(String nature) {
    assertThat(CucumberRestTestContext.getElement("$.natureId")).isEqualTo(naturesDeclarees.get(nature));
    assertThat(CucumberRestTestContext.getElement("$.nature")).isEqualTo(nature);
  }

  /**
   * La nature est declaree dans le referentiel, et son identifiant retenu pour le scenario.
   */
  private String natureDeclaree(String libelle) {
    return naturesDeclarees.computeIfAbsent(libelle, nouvelle -> {
      rest.post("/api/natures-de-travail", JSON.writeValueAsString(Map.of("libelle", nouvelle)));
      assertThatLastResponse().hasHttpStatus(201);
      return (String) CucumberRestTestContext.getElement("$.id");
    });
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
    rest.put(BASE_URI + "/" + dernierIdDeclare, JSON.writeValueAsString(avecIdentifiantDeNature(donnees)));
  }

  @When("je revise le poste de travail {string}")
  public void jeReviseLePosteDeTravail(String id, Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + id, JSON.writeValueAsString(avecIdentifiantDeNature(donnees)));
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

  @Then("la reponse de poste de travail a le cout horaire {string}")
  public void laReponseDePosteALeCoutHoraire(String valeur) {
    Object montant = CucumberRestTestContext.getElement("$.coutHoraire");
    assertThat(new BigDecimal(montant.toString())).isEqualByComparingTo(valeur);
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

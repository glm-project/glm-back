package com.glm.glmback.operateur.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class OperateurSteps {

  private static final String BASE_URI = "/api/operateurs";
  private static final String POSTES_URI = "/api/postes-de-travail";
  private static final String CLE_POSTES = "postes";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  private final Map<String, UUID> postesDeclares = new LinkedHashMap<>();

  private UUID dernierIdDeclare;

  @Given("j'ai un poste de travail {string} de nature {string}")
  public void jaiUnPosteDeTravail(String libelle, String nature) {
    rest.post(POSTES_URI, JSON.writeValueAsString(Map.of("libelle", libelle, "nature", nature)));
    postesDeclares.put(libelle, UUID.fromString((String) CucumberRestTestContext.getElement("$.id")));
  }

  @When("je declare un operateur")
  public void jeDeclareUnOperateur(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(corps(donnees)));
  }

  @Given("j'ai declare un operateur")
  public void jaiDeclareUnOperateur(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(corps(donnees)));
    dernierIdDeclare = UUID.fromString((String) CucumberRestTestContext.getElement("$.id"));
  }

  @When("je consulte cet operateur")
  public void jeConsulteCetOperateur() {
    rest.get(BASE_URI + "/" + dernierIdDeclare);
  }

  @When("je consulte l'operateur {string}")
  public void jeConsulteLOperateur(String id) {
    rest.get(BASE_URI + "/" + id);
  }

  @When("je revise cet operateur")
  public void jeReviseCetOperateur(Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + dernierIdDeclare, JSON.writeValueAsString(corps(donnees)));
  }

  @When("je revise l'operateur {string}")
  public void jeReviseLOperateur(String id, Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + id, JSON.writeValueAsString(corps(donnees)));
  }

  @When("je supprime cet operateur")
  public void jeSupprimeCetOperateur() {
    rest.delete(BASE_URI + "/" + dernierIdDeclare);
  }

  @When("je supprime l'operateur {string}")
  public void jeSupprimeLOperateur(String id) {
    rest.delete(BASE_URI + "/" + id);
  }

  @When("je liste les operateurs")
  public void jeListeLesOperateurs() {
    rest.get(BASE_URI);
  }

  @When("je liste les operateurs habilites sur {string}")
  public void jeListeLesOperateursHabilitesSur(String libelle) {
    rest.get(BASE_URI + "?poste=" + postesDeclares.get(libelle));
  }

  @When("je supprime le poste de travail nomme {string}")
  public void jeSupprimeLePosteDeTravailNomme(String libelle) {
    rest.delete(POSTES_URI + "/" + postesDeclares.get(libelle));
  }

  @Then("la reponse d'operateur contient")
  public void laReponseDOperateurContient(Map<String, Object> attendu) {
    assertThatLastResponse().hasResponse().containing(attendu);
  }

  @Then("la reponse d'operateur n'a pas de matricule")
  public void laReponseDOperateurNAPasDeMatricule() {
    assertThat(CucumberRestTestContext.getElement("$.matricule")).isNull();
  }

  @Then("la reponse d'operateur a les metiers {string}")
  public void laReponseDOperateurALesMetiers(String natures) {
    assertThat(textes("$.natures")).containsExactlyElementsOf(decoupe(natures));
  }

  @Then("la reponse d'operateur a les postes {string}")
  public void laReponseDOperateurALesPostes(String libelles) {
    assertThat(textes("$.postes..libelle")).containsExactlyElementsOf(decoupe(libelles));
  }

  @Then("la reponse contient au moins {int} operateurs")
  public void laReponseContientAuMoinsOperateurs(int count) {
    assertThatLastResponse().hasElement("$.content").withMoreThanElementsCount(count);
  }

  @Then("la reponse ne contient que l'operateur {string}")
  public void laReponseNeContientQueLOperateur(String nom) {
    assertThat(textes("$.content..nom")).containsExactly(nom);
  }

  @SuppressWarnings("unchecked")
  private static List<String> textes(String chemin) {
    return (List<String>) CucumberRestTestContext.getElement(chemin);
  }

  /**
   * La table du scenario est immuable : le corps se construit dans une copie, ou les libelles de postes cedent la
   * place a leurs identifiants.
   */
  private Map<String, Object> corps(Map<String, String> donnees) {
    Map<String, Object> corps = new LinkedHashMap<>(donnees);
    Optional.ofNullable(donnees.get(CLE_POSTES)).ifPresent(libelles -> corps.put(CLE_POSTES, identifiantsDesPostes(libelles)));

    return corps;
  }

  /**
   * Les postes se designent par leur libelle, plus lisible dans un scenario ; un identifiant brut passe tel quel, ce
   * qui laisse eprouver le refus d'un poste inconnu.
   */
  private List<UUID> identifiantsDesPostes(String libelles) {
    return decoupe(libelles)
      .stream()
      .map(libelle -> Optional.ofNullable(postesDeclares.get(libelle)).orElseGet(() -> UUID.fromString(libelle)))
      .toList();
  }

  private static List<String> decoupe(String valeurs) {
    if (valeurs.isBlank()) {
      return List.of();
    }

    return Arrays.stream(valeurs.split(",")).map(String::trim).toList();
  }
}

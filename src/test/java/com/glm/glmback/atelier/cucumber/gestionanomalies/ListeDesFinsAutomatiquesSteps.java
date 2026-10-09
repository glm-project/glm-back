package com.glm.glmback.atelier.cucumber.gestionanomalies;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.atelier.cucumber.AtelierSteps;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class ListeDesFinsAutomatiquesSteps {

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private AtelierSteps atelier;

  @When("je liste les fins automatiques de {string}")
  public void liste(String alias) {
    atelier.jeConsulte(alias);
    var element = (String) CucumberRestTestContext.getElement("$.element");
    rest.get("/api/atelier/anomalies?element=" + element);
  }

  @Then("la liste des fins automatiques compte {int} ligne(s)")
  public void compte(int nombre) {
    assertThatLastResponse().hasOkStatus().hasElement("$.totalElementsCount").withValue(nombre);
    assertThat((List<?>) CucumberRestTestContext.getElement("$.content")).hasSize(nombre);
  }

  @Then("la ligne de la liste des fins automatiques porte")
  public void ligne(Map<String, String> attendu) {
    var lignes = (List<?>) CucumberRestTestContext.getElement("$.content");
    assertThat(lignes).hasSize(1);
    var champs = Map.of(
      "pointage",
      "$.content[0].adresse.pointage",
      "activite",
      "$.content[0].activite",
      "debut",
      "$.content[0].debut",
      "echeance",
      "$.content[0].echeance"
    );
    attendu.forEach((champ, valeur) -> assertThat(CucumberRestTestContext.getElement(champs.get(champ))).isEqualTo(valeur));
  }

  @Then("l'adresse de la premiere ligne de la liste des fins automatiques ouvre le dossier de la meme activite")
  public void adresseOuvreLeDossier() {
    var suivi = (String) CucumberRestTestContext.getElement("$.content[0].adresse.suivi");
    var pointage = (String) CucumberRestTestContext.getElement("$.content[0].adresse.pointage");
    var activite = CucumberRestTestContext.getElement("$.content[0].activite");
    rest.get("/api/atelier/suivis/" + suivi + "/anomalies/" + pointage);
    assertThatLastResponse().hasOkStatus();
    assertThat(CucumberRestTestContext.getElement("$.activite.activite")).isEqualTo(activite);
  }
}

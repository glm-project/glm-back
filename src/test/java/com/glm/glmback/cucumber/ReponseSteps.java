package com.glm.glmback.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;

/**
 * Les assertions de reponse communes a tous les contextes bornes.
 *
 * <p>
 * La glue est scannee depuis la racine {@code com.glm.glmback} : un meme texte de step defini dans deux classes leve
 * une ambiguite. Ce qui ne nomme aucun agregat vit donc ici.
 * </p>
 */
public class ReponseSteps {

  @Then("la reponse ne contient aucun tarif horaire")
  public void laReponseNeContientAucunTarifHoraire() {
    assertThat(CucumberRestTestContext.countEntries("$..tauxHoraire")).isZero();
    assertThat(CucumberRestTestContext.countEntries("$..coutHoraire")).isZero();
  }

  @Then("la reponse a le statut http {int}")
  public void laReponseALeStatutHttp(int status) {
    assertThatLastResponse().hasHttpStatus(status);
  }

  @Then("la reponse porte le code d'erreur {string}")
  public void laReponsePorteLeCodeDErreur(String code) {
    assertThatLastResponse().hasElement("$.type").withValue(code);
  }
}

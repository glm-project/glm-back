package com.glm.glmback.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;

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

  @Then("la reponse a le statut http {int}")
  public void laReponseALeStatutHttp(int status) {
    assertThatLastResponse().hasHttpStatus(status);
  }
}

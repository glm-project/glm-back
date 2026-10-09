package com.glm.glmback.parametrage.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import com.glm.glmback.parametrage.domain.ImagesFixture;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ParametrageSteps {

  private static final String BASE_URI = "/api/parametrage";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final Map<String, Supplier<byte[]>> FICHIERS = Map.of(
    "un PNG de 50 x 50",
    ImagesFixture::pngCarre50,
    "un JPEG de 50 x 50",
    ImagesFixture::jpegCarre50,
    "un GIF de 50 x 50",
    ImagesFixture::gifCarre50,
    "un PNG de 120 x 80",
    ImagesFixture::pngDe120Sur80,
    "un PNG de 50 x 50 de 25 Ko",
    ImagesFixture::pngCarre50Alourdi,
    "un fichier texte",
    ImagesFixture::texte
  );

  @Autowired
  private CucumberRestClient rest;

  @When("je lis le parametrage de l'entreprise")
  public void jeLisLeParametrageDeLEntreprise() {
    rest.get(BASE_URI);
  }

  @When("je fixe la duree max d'une activite a {string}")
  public void jeFixeLaDureeMaxDUneActiviteA(String duree) {
    rest.put(BASE_URI + "/duree-max-d-activite", JSON.writeValueAsString(Map.of("dureeMaxDActivite", duree)));
  }

  @Given("j'ai fixe la duree max d'une activite a {string}")
  public void jaiFixeLaDureeMaxDUneActiviteA(String duree) {
    jeFixeLaDureeMaxDUneActiviteA(duree);
    assertThatLastResponse().hasHttpStatus(200);
  }

  @Then("la duree max d'une activite vaut {string}")
  public void laDureeMaxDUneActiviteVaut(String duree) {
    assertThat(CucumberRestTestContext.getElement("$.dureeMaxDActivite")).isEqualTo(duree);
  }

  @When("je depose comme logo {string}")
  public void jeDeposeCommeLogo(String fichier) {
    rest.putFile(BASE_URI + "/logo", "logo", FICHIERS.get(fichier).get());
  }

  @Then("le logo depose a une version")
  public void leLogoDeposeAUneVersion() {
    assertThat((String) CucumberRestTestContext.getElement("$.version")).matches("^[0-9a-f]{16}$");
  }

  @Then("le refus du logo dit {string}")
  public void leRefusDuLogoDit(String raison) {
    assertThat(CucumberRestTestContext.getElement("$.message")).isEqualTo(raison);
  }
}

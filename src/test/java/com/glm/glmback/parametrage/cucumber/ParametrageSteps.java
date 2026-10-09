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
    "un PNG de 256 x 100",
    ImagesFixture::pngDe256Sur100,
    "un PNG de 300 x 80",
    ImagesFixture::pngDe300Sur80,
    "un PNG de 50 x 50 de 60 Ko",
    ImagesFixture::pngCarre50Alourdi,
    "un fichier texte",
    ImagesFixture::texte
  );

  @Autowired
  private CucumberRestClient rest;

  private String versionRetenue;

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

  @Given("j'ai depose comme logo {string}")
  public void jaiDeposeCommeLogo(String fichier) {
    jeDeposeCommeLogo(fichier);
    assertThatLastResponse().hasHttpStatus(200);
  }

  @When("je lis le logo a sa version")
  public void jeLisLeLogoASaVersion() {
    rest.get(BASE_URI + "/logo/" + versionLue());
  }

  @When("je lis le logo a la version {string}")
  public void jeLisLeLogoALaVersion(String version) {
    rest.get(BASE_URI + "/logo/" + version);
  }

  @When("je retiens la version du logo")
  public void jeRetiensLaVersionDuLogo() {
    versionRetenue = versionLue();
  }

  @When("je lis le logo a la version retenue")
  public void jeLisLeLogoALaVersionRetenue() {
    rest.get(BASE_URI + "/logo/" + versionRetenue);
  }

  @Then("le parametrage porte la version du logo depose")
  public void leParametragePorteLaVersionDuLogoDepose() {
    String deposee = (String) CucumberRestTestContext.getElement("$.version");
    jeLisLeParametrageDeLEntreprise();
    assertThat(CucumberRestTestContext.getElement("$.logo.version")).isEqualTo(deposee);
  }

  @Then("le parametrage n'a pas de logo")
  public void leParametrageNAPasDeLogo() {
    assertThat(CucumberRestTestContext.getElement("$.logo")).isNull();
  }

  @Then("le logo est servi en {string} et garde en cache")
  public void leLogoEstServiEnEtGardeEnCache(String type) {
    assertThat(CucumberRestTestContext.getResponseHeader("Content-Type")).containsExactly(type);
    assertThat(CucumberRestTestContext.getResponseHeader("Cache-Control")).containsExactly("max-age=31536000, private, immutable");
  }

  private String versionLue() {
    jeLisLeParametrageDeLEntreprise();
    return (String) CucumberRestTestContext.getElement("$.logo.version");
  }

  @When("je retire le logo")
  public void jeRetireLeLogo() {
    rest.delete(BASE_URI + "/logo");
  }

  @Then("le referentiel du pupitre porte la version retenue du logo")
  public void leReferentielDuPupitrePorteLaVersionRetenueDuLogo() {
    assertThat(CucumberRestTestContext.getElement("$.logo.version")).isEqualTo(versionRetenue);
  }

  @Then("le referentiel du pupitre n'a pas de logo")
  public void leReferentielDuPupitreNAPasDeLogo() {
    assertThat(CucumberRestTestContext.getElement("$.logo")).isNull();
  }
}

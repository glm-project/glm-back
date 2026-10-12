package com.glm.glmback.elementdefabrication.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.CategoriesDeProduitDesScenarios;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ElementDeFabricationSteps {

  private static final String BASE_URI = "/api/elements-de-fabrication";
  private static final String SUIVIS_URI = "/api/atelier/suivis";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  private UUID dernierIdCree;

  private String derniereCreation;

  private String dernierSuivi;

  @When("je cree un element de fabrication")
  public void jeCreeUnElementDeFabrication(Map<String, String> donnees) {
    Optional.ofNullable(donnees.get("categorie")).ifPresent(categorie -> CategoriesDeProduitDesScenarios.declarer(rest, categorie));
    rest.post(BASE_URI, JSON.writeValueAsString(donnees));
  }

  @When("je cree un element de fabrication sans declarer sa categorie")
  public void jeCreeUnElementDeFabricationSansDeclarerSaCategorie(Map<String, String> donnees) {
    rest.post(BASE_URI, JSON.writeValueAsString(donnees));
  }

  @Given("j'ai cree un element de fabrication")
  public void jaiCreeUnElementDeFabrication(Map<String, String> donnees) {
    jeCreeUnElementDeFabrication(donnees);
    dernierIdCree = idDeLaDerniereReponse();
    derniereCreation = (String) CucumberRestTestContext.getElement("$.dateDeCreation");
  }

  @When("je consulte cet element de fabrication")
  public void jeConsulteCetElementDeFabrication() {
    rest.get(BASE_URI + "/" + dernierIdCree);
  }

  @When("je consulte l'element de fabrication {string}")
  public void jeConsulteLElementDeFabrication(String id) {
    rest.get(BASE_URI + "/" + id);
  }

  @When("je modifie cet element de fabrication")
  public void jeModifieCetElementDeFabrication(Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + dernierIdCree, JSON.writeValueAsString(donnees));
  }

  @When("je modifie cet element de fabrication sans reference ni description")
  public void jeModifieCetElementDeFabricationSansReferenceNiDescription() {
    rest.put(BASE_URI + "/" + dernierIdCree, "{}");
  }

  @When("je modifie l'element de fabrication {string}")
  public void jeModifieLElementDeFabrication(String id, Map<String, String> donnees) {
    rest.put(BASE_URI + "/" + id, JSON.writeValueAsString(donnees));
  }

  @When("je supprime cet element de fabrication")
  public void jeSupprimeCetElementDeFabrication() {
    rest.delete(BASE_URI + "/" + dernierIdCree);
  }

  @When("je supprime l'element de fabrication {string}")
  public void jeSupprimeLElementDeFabrication(String id) {
    rest.delete(BASE_URI + "/" + id);
  }

  @When("je liste les elements de fabrication entre {string} et {string}")
  public void jeListeLesElementsDeFabrication(String debut, String fin) {
    rest.get(BASE_URI + "?debut=" + debut + "&fin=" + fin);
  }

  @Given("j'ai mis cet element de fabrication a l'atelier")
  public void jaiMisCetElementALAtelier() {
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", dernierIdCree)));
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("la mise a l'atelier doit etre acceptee").isTrue();
    dernierSuivi = (String) CucumberRestTestContext.getElement("$.id");
  }

  @Given("j'ai cloture cet element de fabrication")
  public void jaiClotureCetElement() {
    rest.put(SUIVIS_URI + "/" + dernierSuivi + "/cloture", "{}");
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("la cloture doit etre acceptee").isTrue();
  }

  @Given("j'ai rouvert cet element de fabrication")
  public void jaiRouvertCetElement() {
    rest.delete(SUIVIS_URI + "/" + dernierSuivi + "/cloture");
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("la reouverture doit etre acceptee").isTrue();
  }

  /**
   * La liste des elements crees a l'instant meme de cet element : l'horloge des scenarios le rend seul a cet instant.
   */
  @When("je liste les elements de fabrication crees avec celui-ci")
  public void jeListeLesElementsCreesAvecCeluiCi() {
    rest.get(BASE_URI + "?debut=" + derniereCreation + "&fin=" + derniereCreation);
  }

  @When("je liste les elements de fabrication crees avec celui-ci, avec")
  public void jeListeLesElementsCreesAvecCeluiCiAvec(Map<String, String> parametres) {
    rest.get(avec(BASE_URI + "?debut=" + derniereCreation + "&fin=" + derniereCreation, parametres));
  }

  @When("je liste les elements de fabrication avec")
  public void jeListeLesElementsDeFabricationAvec(Map<String, String> parametres) {
    rest.get(avec(BASE_URI + "?page=0", parametres));
  }

  private static String avec(String uri, Map<String, String> parametres) {
    StringBuilder complete = new StringBuilder(uri);
    parametres.forEach((nom, valeur) -> complete.append('&').append(nom).append('=').append(valeur));
    return complete.toString();
  }

  @Then("la liste ne rend que les references")
  public void laListeNeRendQueLesReferences(List<String> references) {
    assertThat(CucumberRestTestContext.getElement("$.content[*].reference"))
      .asInstanceOf(LIST)
      .containsExactlyInAnyOrderElementsOf(references);
  }

  @Then("cet element de fabrication n'est liste que dans le cycle {string}")
  public void cetElementNEstListeQueDansLeCycle(String attendu) {
    for (String cycle : List.of("EN_PREPARATION", "EN_ATELIER", "CLOTURE")) {
      rest.get(BASE_URI + "?debut=" + derniereCreation + "&fin=" + derniereCreation + "&cycle=" + cycle);
      assertThat(CucumberRestTestContext.countEntries("$.content")).as(cycle).isEqualTo(cycle.equals(attendu) ? 1 : 0);
    }
  }

  /**
   * Compare la seule ligne listee aux champs attendus ; « - » dit un champ absent, « suivi » le suivi mis a l'atelier.
   */
  @Then("cet element de fabrication est liste avec")
  public void cetElementEstListeAvec(Map<String, String> attendu) {
    assertThat(CucumberRestTestContext.countEntries("$.content")).isEqualTo(1);
    assertThat(CucumberRestTestContext.getElement("$.content[0].id")).isEqualTo(dernierIdCree.toString());
    attendu.forEach((champ, valeur) -> {
      Object lu = CucumberRestTestContext.getElement("$.content[0]." + champ);
      switch (valeur) {
        case "-" -> assertThat(lu).as(champ).isNull();
        case "suivi" -> assertThat(lu).as(champ).isEqualTo(dernierSuivi);
        default -> assertThat(lu).as(champ).hasToString(valeur);
      }
    });
  }

  @Then("la reponse d'element de fabrication contient")
  public void laReponseDElementDeFabricationContient(Map<String, Object> attendu) {
    assertThatLastResponse().hasResponse().containing(attendu);
  }

  @Then("la reponse d'element de fabrication a un nom commencant par {string}")
  public void laReponseAUnNomCommencantPar(String prefixe) {
    assertThat((String) CucumberRestTestContext.getElement("$.nom")).startsWith(prefixe);
  }

  @Then("la reponse d'element de fabrication n'a ni reference ni description")
  public void laReponseDElementDeFabricationNAPasDeReferenceNiDescription() {
    assertThat(CucumberRestTestContext.getElement("$.reference")).isNull();
    assertThat(CucumberRestTestContext.getElement("$.description")).isNull();
  }

  @Then("la reponse contient au moins {int} elements")
  public void laReponseContientAuMoins(int count) {
    assertThatLastResponse().hasElement("$.content").withMoreThanElementsCount(count);
  }

  private UUID idDeLaDerniereReponse() {
    return UUID.fromString((String) CucumberRestTestContext.getElement("$.id"));
  }
}

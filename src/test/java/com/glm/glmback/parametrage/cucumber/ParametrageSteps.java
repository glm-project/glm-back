package com.glm.glmback.parametrage.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import com.glm.glmback.parametrage.domain.AmplitudeMaximale;
import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ParametrageSteps {

  private static final String BASE_URI = "/api/parametrage";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final List<String> ENTREPRISES = List.of("impeccmold", "katilys");

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private ParametrageRepository parametrages;

  @Autowired
  private TransactionTemplate transactions;

  /**
   * Le parametrage est une ligne unique par entreprise, partagee par tous les scenarios : ceux qui la modifient la
   * remettent a sa valeur semee, sans trace, pour que les autres contextes continuent de lire 13 h.
   *
   * <p>
   * Le schema de l'entreprise ne se resout qu'au sein d'une requete : hors de l'API, le hook en simule une le temps
   * de la remise a zero.
   * </p>
   */
  @After("@parametrage")
  public void remetLeParametrageSeme() {
    SecurityContext appelant = SecurityContextHolder.getContext();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    try {
      ENTREPRISES.forEach(entreprise -> {
        TenantSecurityContexts.authenticateOn(entreprise);
        transactions.executeWithoutResult(status ->
          parametrages.update(new Parametrage(new AmplitudeMaximale(Duration.ofHours(13)), Optional.empty()))
        );
      });
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.setContext(appelant);
    }
  }

  @When("je consulte le parametrage")
  public void jeConsulteLeParametrage() {
    rest.get(BASE_URI);
  }

  @When("je fixe l'amplitude maximale a {string}")
  public void jeFixeLAmplitudeMaximaleA(String valeur) {
    rest.put(BASE_URI + "/amplitude-maximale", JSON.writeValueAsString(Map.of("valeur", valeur)));
  }

  @When("je fixe l'amplitude maximale avec le corps")
  public void jeFixeLAmplitudeMaximaleAvecLeCorps(String corps) {
    rest.put(BASE_URI + "/amplitude-maximale", corps);
  }

  @Then("l'amplitude maximale est {string}")
  public void lAmplitudeMaximaleEst(String valeur) {
    assertThatLastResponse().hasElement("$.amplitudeMaximale").withValue(valeur);
  }

  @Then("le parametrage n'a jamais ete modifie")
  public void leParametrageNAJamaisEteModifie() {
    JsonNode modification = derniereReponse().path("derniereModification");

    assertThat(modification.isMissingNode() || modification.isNull()).isTrue();
  }

  @Then("la derniere modification du parametrage est de {string} a {string}")
  public void laDerniereModificationDuParametrageEstDeA(String auteur, String date) {
    assertThatLastResponse().hasElement("$.derniereModification.auteur").withValue(auteur);
    assertThatLastResponse().hasElement("$.derniereModification.date").withValue(date);
  }

  private static JsonNode derniereReponse() {
    return JSON.readTree(CucumberRestTestContext.getResponse().orElseThrow());
  }
}

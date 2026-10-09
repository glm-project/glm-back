package com.glm.glmback.atelier.cucumber.gestionanomalies;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.atelier.cucumber.AtelierSteps;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * La lecture du dossier d'une fin automatique, ouvert depuis l'ouvrant de l'activite echue, et la borne qu'il donne a
 * sa regularisation.
 *
 * <p>
 * Les evenements se designent par leur rang dans le journal du suivi : les identifiants d'evenement et d'activite
 * viennent du serveur, jamais d'une valeur supposee.
 * </p>
 */
public class DossierDeFinAutomatiqueSteps {

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private AtelierSteps atelier;

  private Map<String, Object> suivi;
  private List<Map<String, Object>> journal;

  @When("je consulte le dossier d'anomalie de {string} depuis l'evenement {int}")
  @SuppressWarnings("unchecked")
  public void consulte(String alias, int rang) {
    atelier.jeConsulte(alias);
    var suiviLu = (Map<String, Object>) CucumberRestTestContext.getElement("$");
    suivi = suiviLu;
    journal = (List<Map<String, Object>>) suiviLu.get("journal");
    rest.get("/api/atelier/suivis/" + suiviLu.get("id") + "/anomalies/" + journal.get(rang).get("id"));
  }

  @Then("le dossier d'anomalie donne l'activite")
  public void activite(List<Map<String, String>> attendue) {
    attendue
      .getFirst()
      .forEach((champ, valeur) -> {
        var lue = "evenement".equals(champ)
          ? String.valueOf(rangDeLActivite((String) CucumberRestTestContext.getElement("$.activite.activite")))
          : CucumberRestTestContext.getElement("$.activite." + champ);
        assertThat(lue).as(champ).hasToString(valeur);
      });
  }

  @Then("le dossier d'anomalie donne l'element du suivi")
  public void element() {
    assertThat(CucumberRestTestContext.getElement("$.designation")).hasToString(String.valueOf(suivi.get("nom")));
    assertThat(CucumberRestTestContext.getElement("$.elementId")).hasToString(String.valueOf(suivi.get("element")));
  }

  @Then("le dossier d'anomalie donne les pointages des evenements {string}")
  public void pointages(String rangs) {
    var attendus = Arrays.stream(rangs.split(","))
      .map(rang -> journal.get(Integer.parseInt(rang.trim())).get("id"))
      .toList();

    assertThat((List<?>) CucumberRestTestContext.getElement("$.pointages[*].id")).isEqualTo(attendus);
  }

  @Then("le dossier d'anomalie donne la borne de fin {string}")
  public void borneDeFin(String attendue) {
    assertThat(CucumberRestTestContext.getElement("$.borneDeFin")).hasToString(attendue);
  }

  @Then("le dossier d'anomalie ne donne aucune borne de fin")
  public void aucuneBorneDeFin() {
    assertThat(CucumberRestTestContext.getElement("$.borneDeFin")).isNull();
  }

  private int rangDeLActivite(String activite) {
    return journal
      .stream()
      .map(fait -> fait.get("activite"))
      .toList()
      .indexOf(activite);
  }
}

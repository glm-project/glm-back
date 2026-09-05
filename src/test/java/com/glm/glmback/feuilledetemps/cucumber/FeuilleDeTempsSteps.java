package com.glm.glmback.feuilledetemps.cucumber;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.CucumberClock;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * La feuille de temps vue du client HTTP, du pointage a sa relecture calendaire.
 *
 * <p>
 * Tout passe par les API : le referentiel pour declarer l'operateur, l'atelier pour pointer, la feuille de temps pour
 * relire. C'est ce qui fait de ce scenario la garantie que les deux contextes lisent bien les memes tables — aucun
 * import Java ne relie {@code feuilledetemps} a {@code atelier}.
 * </p>
 */
public class FeuilleDeTempsSteps {

  private static final String OPERATEURS_URI = "/api/operateurs";
  private static final String JOURNEES_URI = "/api/atelier/journees";
  private static final String FEUILLES_URI = "/api/feuilles-de-temps";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  private final Map<String, String> operateurs = new HashMap<>();

  @Given("la feuille de temps suit l'operateur {string}")
  public void laFeuilleDeTempsSuitLOperateur(String alias) {
    Map<String, Object> corps = Map.of("nom", alias, "prenom", "Feuille " + SEQUENCE.incrementAndGet(), "postes", List.of());
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, String.valueOf(CucumberRestTestContext.getElement("$.id")));
  }

  @Given("{string} est arrive a {string}")
  public void estArriveA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(JOURNEES_URI, JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "operateur", operateurs.get(alias))));
  }

  @Given("{string} a pointe {string} a {string}")
  public void aPointeA(String alias, String type, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(
      JOURNEES_URI + "/pointages",
      JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "operateur", operateurs.get(alias), "type", type))
    );
  }

  @When("je consulte la feuille de temps de {string} pour la semaine {int} de {int}")
  public void jeConsulteLaFeuilleDeTempsDe(String alias, int semaine, int annee) {
    consulte(operateurs.get(alias), semaine, annee);
  }

  @When("je consulte la feuille de temps de l'operateur {string} pour la semaine {int} de {int}")
  public void jeConsulteLaFeuilleDeTempsDeLOperateur(String operateur, int semaine, int annee) {
    consulte(operateur, semaine, annee);
  }

  @Then("la feuille de temps porte les jours")
  public void laFeuilleDeTempsPorteLesJours(List<String> attendus) {
    assertThat(jours())
      .extracting(jour -> jour.get("jour"))
      .containsExactlyElementsOf(attendus);
  }

  @Then("la feuille de temps ne porte aucune presence")
  public void laFeuilleDeTempsNePorteAucunePresence() {
    assertThat(jours()).allSatisfy(jour -> assertThat(presenceDe(jour)).isEmpty());
  }

  @Then("la presence du {string} est")
  public void laPresenceDuEst(String jour, List<Map<String, String>> attendues) {
    assertThat(presenceDu(jour)).isEqualTo(attendues);
  }

  @Then("la presence du {string} est vide")
  public void laPresenceDuEstVide(String jour) {
    assertThat(presenceDu(jour)).isEmpty();
  }

  @Then("la presence du {string} commence a {string} et n'est pas terminee")
  public void laPresenceDuCommenceAEtNEstPasTerminee(String jour, String debut) {
    assertThat(presenceDu(jour)).hasSize(1);
    assertThat(presenceDu(jour).getFirst()).containsOnly(entry("debut", debut));
  }

  private void consulte(String operateur, int semaine, int annee) {
    rest.get(FEUILLES_URI + "/" + operateur + "?annee=" + annee + "&semaine=" + semaine);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> jours() {
    return (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours");
  }

  private static List<Map<String, String>> presenceDu(String jour) {
    return jours()
      .stream()
      .filter(jourDeLaSemaine -> jour.equals(jourDeLaSemaine.get("jour")))
      .findFirst()
      .map(FeuilleDeTempsSteps::presenceDe)
      .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, String>> presenceDe(Map<String, Object> jour) {
    return (List<Map<String, String>>) jour.get("presence");
  }
}

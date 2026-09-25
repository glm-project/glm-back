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
  private static final String ELEMENTS_URI = "/api/elements-de-fabrication";
  private static final String SUIVIS_URI = "/api/atelier/suivis";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  private final Map<String, String> operateurs = new HashMap<>();
  private final Map<String, String> journees = new HashMap<>();

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
    journees.put(alias, String.valueOf(CucumberRestTestContext.getElement("$.id")));
  }

  /**
   * Un ordre cree, engage et demarre a cet instant par l'operateur : de quoi donner a une journee abandonnee un
   * dernier fait connu d'atelier, que le releve doit retrouver pour presumer sa fin.
   */
  @Given("{string} a demarre un ordre de fabrication a {string}")
  public void aDemarreUnOrdreDeFabricationA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    Map<String, Object> element = Map.of("type", "ORDRE_DE_FABRICATION", "reference", "FEUILLE-" + SEQUENCE.incrementAndGet());
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(element));
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", String.valueOf(CucumberRestTestContext.getElement("$.id")))));
    String suivi = String.valueOf(CucumberRestTestContext.getElement("$.id"));
    rest.post(
      SUIVIS_URI + "/" + suivi + "/pointages",
      JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "type", "DEBUT", "operateur", operateurs.get(alias)))
    );
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("le pointage de l'ordre doit etre accepte").isTrue();
  }

  @Given("le depart de {string} est regularise a {string}")
  public void leDepartDeEstRegulariseA(String alias, String instant) {
    rest.post(
      JOURNEES_URI + "/" + journees.get(alias) + "/regularisations",
      JSON.writeValueAsString(Map.of("type", "DEPART", "dateDeSurvenue", instant))
    );
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
    List<Map<String, String>> presence = presenceDu(jour);

    assertThat(presence).hasSameSizeAs(attendues);
    for (int rang = 0; rang < attendues.size(); rang++) {
      Map<String, String> attendue = attendues.get(rang);
      Map<String, String> lue = presence.get(rang);
      attendue.forEach((cle, valeur) -> assertThat(String.valueOf(lue.get(cle))).as(cle).isEqualTo(valeur));
    }
  }

  @Then("la presence du {string} est vide")
  public void laPresenceDuEstVide(String jour) {
    assertThat(presenceDu(jour)).isEmpty();
  }

  @Then("la presence du {string} commence a {string} et n'est pas terminee")
  public void laPresenceDuCommenceAEtNEstPasTerminee(String jour, String debut) {
    assertThat(presenceDu(jour)).hasSize(1);
    assertThat(presenceDu(jour).getFirst()).containsEntry("debut", debut).doesNotContainKey("fin");
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

package com.glm.glmback.syntheseheures.cucumber;

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
 * La synthese des heures vue du client HTTP, du pointage a sa relecture calendaire.
 *
 * <p>
 * Tout passe par les API : le referentiel pour declarer l'operateur, l'atelier pour pointer, la synthese des heures
 * pour relire. C'est ce qui fait de ce scenario la garantie que les deux contextes lisent bien les memes tables —
 * aucun import Java ne relie {@code syntheseheures} a {@code atelier}.
 * </p>
 */
public class SyntheseDesHeuresSteps {

  private static final String OPERATEURS_URI = "/api/operateurs";
  private static final String JOURNEES_URI = "/api/atelier/journees";
  private static final String SYNTHESES_URI = "/api/syntheses-des-heures";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  private final Map<String, String> operateurs = new HashMap<>();

  @Given("la synthese des heures suit l'operateur {string}")
  public void laSyntheseDesHeuresSuitLOperateur(String alias) {
    Map<String, Object> corps = Map.of("nom", alias, "prenom", "Synthese " + SEQUENCE.incrementAndGet(), "postes", List.of());
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, String.valueOf(CucumberRestTestContext.getElement("$.id")));
  }

  @Given("{string} pointe son arrivee a {string}")
  public void pointeSonArriveeA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(JOURNEES_URI, JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "operateur", operateurs.get(alias))));
  }

  @Given("{string} enregistre le pointage {string} a {string}")
  public void enregistreLePointageA(String alias, String type, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(
      JOURNEES_URI + "/pointages",
      JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "operateur", operateurs.get(alias), "type", type))
    );
  }

  @When("je consulte la synthese des heures de {string} pour la semaine {int} de {int}")
  public void jeConsulteLaSyntheseDesHeuresDe(String alias, int semaine, int annee) {
    consulte(operateurs.get(alias), semaine, annee);
  }

  @When("je consulte la synthese des heures de l'operateur {string} pour la semaine {int} de {int}")
  public void jeConsulteLaSyntheseDesHeuresDeLOperateur(String operateur, int semaine, int annee) {
    consulte(operateur, semaine, annee);
  }

  @Then("la synthese des heures porte les jours")
  public void laSyntheseDesHeuresPorteLesJours(List<String> attendus) {
    assertThat(jours())
      .extracting(jour -> jour.get("jour"))
      .containsExactlyElementsOf(attendus);
  }

  @Then("chaque jour de la synthese ne porte aucun pointage et une duree de {string}")
  public void chaqueJourNePorteAucunPointageEtUneDureeDe(String duree) {
    assertThat(jours()).allSatisfy(jour -> {
      assertThat(pointagesDe(jour)).isEmpty();
      assertThat(jour.get("duree")).isEqualTo(duree);
    });
  }

  @Then("les pointages du {string} sont")
  public void lesPointagesDuSont(String jour, List<Map<String, String>> attendus) {
    List<Map<String, String>> pointages = pointagesDu(jour)
      .stream()
      .map(pointage ->
        Map.of("type", String.valueOf(pointage.get("type")), "dateDeSurvenue", String.valueOf(pointage.get("dateDeSurvenue")))
      )
      .toList();

    assertThat(pointages).isEqualTo(attendus);
  }

  @Then("le jour {string} a une duree de {string}")
  public void leJourADuneDureeDe(String jour, String duree) {
    assertThat(jourDe(jour).get("duree")).isEqualTo(duree);
  }

  @Then("la duree totale de la semaine est {string}")
  public void laDureeTotaleDeLaSemaineEst(String duree) {
    assertThat(CucumberRestTestContext.getElement("$.dureeTotale")).isEqualTo(duree);
  }

  private void consulte(String operateur, int semaine, int annee) {
    rest.get(SYNTHESES_URI + "/" + operateur + "?annee=" + annee + "&semaine=" + semaine);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> jours() {
    return (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours");
  }

  private static Map<String, Object> jourDe(String jour) {
    return jours()
      .stream()
      .filter(jourDeSynthese -> jour.equals(jourDeSynthese.get("jour")))
      .findFirst()
      .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> pointagesDe(Map<String, Object> jour) {
    return (List<Map<String, Object>>) jour.get("pointages");
  }

  private static List<Map<String, Object>> pointagesDu(String jour) {
    return pointagesDe(jourDe(jour));
  }
}

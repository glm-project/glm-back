package com.glm.glmback.coutderevient.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.CucumberClock;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le cout de revient vu du client HTTP, du pointage a sa relecture valorisee.
 *
 * <p>
 * Tout passe par les API : le referentiel pour declarer le poste et l'operateur avec leurs tarifs, l'atelier pour
 * engager et pointer, le cout de revient pour relire. C'est ce qui fait de ce scenario la garantie que les deux
 * contextes lisent bien les memes tables — aucun import Java ne relie {@code coutderevient} a {@code atelier}.
 * </p>
 *
 * <p>
 * Les steps portent une formulation qui leur est propre : la glue Cucumber est globale, et reutiliser celle de
 * l'atelier ferait dependre ce scenario de l'etat prive d'une autre classe.
 * </p>
 */
public class CoutDeRevientSteps {

  private static final String SUIVIS_URI = "/api/atelier/suivis";
  private static final String JOURNEES_URI = "/api/atelier/journees";
  private static final String ELEMENTS_URI = "/api/elements-de-fabrication";
  private static final String POSTES_URI = "/api/postes-de-travail";
  private static final String OPERATEURS_URI = "/api/operateurs";
  private static final String RAPPORTS_URI = "/api/couts-de-revient";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> operateurs = new HashMap<>();
  private final Map<String, String> elements = new HashMap<>();
  private final Map<String, String> suivis = new HashMap<>();

  @Given("le rapport connait le poste {string} de nature {string} a {string} de l'heure")
  public void leRapportConnaitLePoste(String alias, String nature, String coutHoraire) {
    Map<String, Object> corps = Map.of("libelle", alias + " " + SEQUENCE.incrementAndGet(), "nature", nature, "coutHoraire", coutHoraire);
    rest.post(POSTES_URI, JSON.writeValueAsString(corps));
    postes.put(alias, id());
  }

  @Given("le rapport connait l'operateur {string} a {string} de l'heure, habilite sur")
  public void leRapportConnaitLOperateur(String alias, String tauxHoraire, List<String> habilitations) {
    Map<String, Object> corps = Map.of(
      "nom",
      alias,
      "prenom",
      "Cout " + SEQUENCE.incrementAndGet(),
      "postes",
      habilitations.stream().map(postes::get).toList(),
      "tauxHoraire",
      tauxHoraire
    );
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, id());
  }

  @Given("l'entreprise fabrique {string}")
  public void lEntrepriseFabrique(String alias) {
    Map<String, Object> corps = Map.of("type", "ORDRE_DE_FABRICATION", "reference", alias + " " + SEQUENCE.incrementAndGet());
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(corps));
    elements.put(alias, id());
  }

  /**
   * L'instant de l'engagement est explicite : l'atelier refuse tout evenement anterieur, et l'horloge des scenarios
   * repart de l'heure reelle a chaque scenario.
   */
  @Given("{string} est mis en atelier a {string}")
  public void estMisEnAtelierA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", elements.get(alias))));
    suivis.put(alias, id());
  }

  @Given("{string} prend son poste a {string}")
  public void prendSonPosteA(String operateur, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(JOURNEES_URI, JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "operateur", operateurs.get(operateur))));
  }

  @Given("{string} pointe sa presence {string} a {string}")
  public void pointeSaPresenceA(String operateur, String type, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(
      JOURNEES_URI + "/pointages",
      JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "operateur", operateurs.get(operateur), "type", type))
    );
  }

  @Given("{string} pointe {string} sur {string} au poste {string} a {string}")
  public void pointeSurAuPoste(String operateur, String type, String element, String poste, String instant) {
    horloge.ilEst(Instant.parse(instant));
    Map<String, Object> corps = Map.of(
      "id",
      UUID.randomUUID(),
      "type",
      type,
      "operateur",
      operateurs.get(operateur),
      "poste",
      postes.get(poste)
    );
    rest.post(SUIVIS_URI + "/" + suivis.get(element) + "/pointages", JSON.writeValueAsString(corps));
  }

  @Given("{string} pointe {string} sur {string} sans poste a {string}")
  public void pointeSurSansPoste(String operateur, String type, String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    Map<String, Object> corps = Map.of("id", UUID.randomUUID(), "type", type, "operateur", operateurs.get(operateur));
    rest.post(SUIVIS_URI + "/" + suivis.get(element) + "/pointages", JSON.writeValueAsString(corps));
  }

  @Given("{string} est cloture a {string}")
  public void estClotureA(String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.put(SUIVIS_URI + "/" + suivis.get(element) + "/cloture", JSON.writeValueAsString(Map.of("dateDeSurvenue", instant)));
  }

  @When("je consulte le cout de revient de {string} a {string}")
  public void jeConsulteLeCoutDeRevientA(String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.get(RAPPORTS_URI + "/" + elements.get(element));
  }

  @When("je consulte le cout de revient de l'element inconnu {string}")
  public void jeConsulteLeCoutDeRevientInconnu(String id) {
    rest.get(RAPPORTS_URI + "/" + id);
  }

  @Then("le rapport ne porte aucune ligne")
  public void leRapportNePorteAucuneLigne() {
    assertThatLastResponse().hasElement("$.lignes").withElementsCount(0);
  }

  @Then("le rapport porte les lignes")
  public void leRapportPorteLesLignes(List<Map<String, String>> attendues) {
    assertThat(lignes()).isEqualTo(attendues);
  }

  @Then("le cout total du rapport est {string} dont {string} de machine")
  public void leCoutTotalDuRapportEst(String total, String machine) {
    assertThat(montant(CucumberRestTestContext.getElement("$.cout.total"))).isEqualTo(total);
    assertThat(montant(CucumberRestTestContext.getElement("$.cout.machine"))).isEqualTo(machine);
  }

  @Then("le rapport porte la non conformite de {string} a {string}")
  public void leRapportPorteLaNonConformite(String debut, String fin) {
    assertThatLastResponse()
      .hasElement("$.lignes[0].nonConformites[0].debut")
      .withValue(debut)
      .and()
      .hasElement("$.lignes[0].nonConformites[0].fin")
      .withValue(fin);
  }

  /**
   * Chaque ligne reduite a ce qui se verifie a la main : la nature, les deux temps, et les deux couts.
   */
  @SuppressWarnings("unchecked")
  private static List<Map<String, String>> lignes() {
    List<Map<String, Object>> lues = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.lignes");
    List<Map<String, String>> lignes = new ArrayList<>();

    lues.forEach(ligne -> {
      Map<String, String> resume = new java.util.LinkedHashMap<>();
      resume.put("nature", String.valueOf(ligne.get("nature")));
      resume.put("travail", String.valueOf(((Map<String, Object>) ligne.get("temps")).get("travail")));
      resume.put("nonConformite", String.valueOf(((Map<String, Object>) ligne.get("temps")).get("nonConformite")));
      resume.put("machine", montant(((Map<String, Object>) ligne.get("cout")).get("machine")));
      resume.put("mainDOeuvre", montant(((Map<String, Object>) ligne.get("cout")).get("mainDOeuvre")));
      lignes.add(resume);
    });

    return lignes;
  }

  /**
   * Les montants voyagent en nombres JSON, et le client de test les relit en {@code double} : leur zero final se
   * perd en chemin. Le domaine, lui, arrondit bien au centime — c'est cette valeur-la que les scenarios enoncent.
   */
  private static String montant(Object valeur) {
    return new java.math.BigDecimal(String.valueOf(valeur)).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private static String id() {
    return (String) CucumberRestTestContext.getElement("$.id");
  }
}

package com.glm.glmback.coutderevient.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.CucumberClock;
import com.glm.glmback.cucumber.EcrituresDuJournalDAtelier;
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

  @Autowired
  private EcrituresDuJournalDAtelier ecritures;

  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> operateurs = new HashMap<>();
  private final Map<String, String> elements = new HashMap<>();
  private final Map<String, String> suivis = new HashMap<>();
  private final Map<String, String> pointages = new HashMap<>();
  private final Map<String, Map<String, Object>> corpsDesPointages = new HashMap<>();

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
    ecritures.pointe(suivis.get(element), corps);
  }

  @Given("{string} pointe {string} sur {string} sans poste a {string}")
  public void pointeSurSansPoste(String operateur, String type, String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    Map<String, Object> corps = Map.of("id", UUID.randomUUID(), "type", type, "operateur", operateurs.get(operateur));
    ecritures.pointe(suivis.get(element), corps);
  }

  @Given("{string} est cloture a {string}")
  public void estClotureA(String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.put(SUIVIS_URI + "/" + suivis.get(element) + "/cloture", JSON.writeValueAsString(Map.of("dateDeSurvenue", instant)));
  }

  @Given("pour le cout, {string} recoit les pointages")
  public void recoitLesPointages(String element, List<Map<String, String>> recus) {
    for (Map<String, String> pointage : recus) {
      String survenue = pointage.get("survenue");
      horloge.ilEst(Instant.parse(java.util.Optional.ofNullable(pointage.get("reception")).orElse(survenue)));
      String identite = UUID.randomUUID().toString();
      Map<String, Object> corps = new HashMap<>();
      corps.put("id", identite);
      corps.put("type", pointage.get("type"));
      corps.put("intention", pointage.get("intention"));
      corps.put("operateur", operateurs.get(pointage.get("operateur")));
      corps.put("dateDeSurvenue", survenue);
      if (pointage.containsKey("poste")) {
        corps.put("poste", postes.get(pointage.get("poste")));
      }
      if (!java.util.Optional.ofNullable(pointage.get("cible")).orElse("").isEmpty()) {
        corps.put("cible", pointages.get(pointage.get("cible")));
      }
      if ("REGULARISATION".equals(pointage.get("acte"))) {
        corps.remove("id");
        ecritures.regularise(suivis.get(element), corps);
      } else {
        ecritures.pointe(suivis.get(element), corps);
      }
      assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("le pointage explicite du cout doit etre accepte").isTrue();
      if ("REGULARISATION".equals(pointage.get("acte"))) {
        identite = identiteDuPointageActif(survenue, pointage.get("type"), pointage.get("intention"));
      }
      pointages.put(pointage.get("alias"), identite);
      corpsDesPointages.put(pointage.get("alias"), corps);
    }
  }

  @SuppressWarnings("unchecked")
  private static String identiteDuPointageActif(String survenue, String type, String intention) {
    List<Map<String, Object>> journal = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.journal");
    return journal
      .stream()
      .filter(
        pointage ->
          survenue.equals(pointage.get("dateDeSurvenue"))
          && type.equals(pointage.get("type"))
          && intention.equals(pointage.get("intention"))
      )
      .filter(pointage -> pointage.get("annulation") == null)
      .map(pointage -> (String) pointage.get("id"))
      .findFirst()
      .orElseThrow();
  }

  @Given("pour le cout, le pointage {string} sur {string} est annule a {string}")
  public void annule(String alias, String element, String reception) {
    horloge.ilEst(Instant.parse(reception));
    rest.post(
      SUIVIS_URI + "/" + suivis.get(element) + "/evenements/" + pointages.get(alias) + "/annulation",
      JSON.writeValueAsString(Map.of("motif", "saisie en trop"))
    );
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("annulation acceptee").isTrue();
  }

  @Given("pour le cout, le pointage {string} sur {string} est corrige a {string} vers {string}")
  public void corrige(String alias, String element, String reception, String survenue) {
    Map<String, Object> corps = new HashMap<>(corpsDesPointages.get(alias));
    corps.put("dateDeSurvenue", survenue);
    corrige(alias, element, reception, corps);
  }

  @Given("pour le cout, la cible du pointage {string} sur {string} est corrigee vers {string} a {string}")
  public void corrigeCible(String alias, String element, String cible, String reception) {
    Map<String, Object> corps = new HashMap<>(corpsDesPointages.get(alias));
    corps.put("cible", pointages.get(cible));
    corrige(alias, element, reception, corps);
  }

  private void corrige(String alias, String element, String reception, Map<String, Object> corps) {
    horloge.ilEst(Instant.parse(reception));
    corps.remove("id");
    corps.put("motif", "saisie erronee");
    ecritures.corrige(suivis.get(element), pointages.get(alias), corps);
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("correction acceptee").isTrue();
    pointages.put(
      alias + "-corrige",
      identiteDuPointageActif((String) corps.get("dateDeSurvenue"), (String) corps.get("type"), (String) corps.get("intention"))
    );
  }

  @Given("pour le cout, le poste {string} est revise a {string} de l'heure et l'operateur {string} a {string} a {string}")
  @SuppressWarnings("unchecked")
  public void reviseTarifs(String poste, String cout, String operateur, String taux, String reception) {
    horloge.ilEst(Instant.parse(reception));
    rest.get(POSTES_URI + "/" + postes.get(poste));
    Map<String, Object> fichePoste = new HashMap<>((Map<String, Object>) CucumberRestTestContext.getElement("$"));
    fichePoste.put("coutHoraire", cout);
    rest.put(POSTES_URI + "/" + postes.get(poste), JSON.writeValueAsString(fichePoste));
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).isTrue();
    rest.get(OPERATEURS_URI + "/" + operateurs.get(operateur));
    Map<String, Object> ficheOperateur = new HashMap<>((Map<String, Object>) CucumberRestTestContext.getElement("$"));
    ficheOperateur.put("postes", List.of(postes.get("fraiseuse"), postes.get("tour")));
    ficheOperateur.put("tauxHoraire", taux);
    rest.put(OPERATEURS_URI + "/" + operateurs.get(operateur), JSON.writeValueAsString(ficheOperateur));
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).isTrue();
  }

  @Then("le cout ne porte aucun conflit")
  public void sansConflit() {
    assertThat((List<?>) CucumberRestTestContext.getElement("$.conflits")).isEmpty();
  }

  @Then("le cout porte {int} fins automatiques")
  @SuppressWarnings("unchecked")
  public void nombreFinsAutomatiques(int nombre) {
    List<Map<String, Object>> lignes = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.lignes");
    assertThat(
      lignes
        .stream()
        .mapToInt(ligne -> ((List<?>) ligne.get("finsAutomatiques")).size())
        .sum()
    ).isEqualTo(nombre);
  }

  @Then("le cout ne porte aucune fin automatique")
  @SuppressWarnings("unchecked")
  public void sansFinAutomatique() {
    List<Map<String, Object>> lignes = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.lignes");
    assertThat(lignes).allSatisfy(ligne -> assertThat((List<?>) ligne.get("finsAutomatiques")).isEmpty());
  }

  @Then("le cout porte les conflits")
  @SuppressWarnings("unchecked")
  public void conflits(List<Map<String, String>> attendus) {
    List<Map<String, Object>> lus = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.conflits");
    List<Map<String, Object>> esperes = attendus
      .stream()
      .map(ligne -> {
        Map<String, Object> attendu = new java.util.LinkedHashMap<>();
        attendu.put("element", elements.get(ligne.get("element")));
        attendu.put("operateur", operateurs.get(ligne.get("operateur")));
        if (ligne.get("poste") != null) {
          attendu.put("poste", postes.get(ligne.get("poste")));
        }
        attendu.put("activites", identites(ligne.get("activites")));
        attendu.put("pointages", identites(ligne.get("pointages")));
        return attendu;
      })
      .toList();
    assertThat(lus).containsExactlyInAnyOrderElementsOf(esperes);
  }

  private List<String> identites(String aliases) {
    return aliases == null || aliases.isEmpty() ? List.of() : java.util.Arrays.stream(aliases.split(",")).map(pointages::get).toList();
  }

  @Then("le total du cout {string} est incomplet sans chiffre")
  public void incomplet(String chemin) {
    assertThat(CucumberRestTestContext.getElement(chemin)).isEqualTo(Map.of("complete", false));
  }

  @Then("le total du cout {string} est complet avec {string}")
  public void complet(String chemin, String valeur) {
    assertThat(CucumberRestTestContext.getElement(chemin + ".complete")).isEqualTo(true);
    Object lue = CucumberRestTestContext.getElement(chemin + ".valeur");
    assertThat(lue instanceof Number ? montant(lue) : String.valueOf(lue)).isEqualTo(valeur);
  }

  @Then("le cout porte la fin automatique de {string} a {string}")
  public void finAutomatique(String debut, String fin) {
    assertThatLastResponse()
      .hasElement("$.lignes[0].finsAutomatiques[0].debut")
      .withValue(debut)
      .and()
      .hasElement("$.lignes[0].finsAutomatiques[0].fin")
      .withValue(fin);
  }

  @Then("le cout est evalue a {string} avec {int} activites en cours exclues")
  public void evaluation(String instant, int enCours) {
    assertThat(CucumberRestTestContext.getElement("$.evaluation")).isEqualTo(instant);
    assertThat(CucumberRestTestContext.getElement("$.activitesEnCours")).isEqualTo(enCours);
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
      resume.put("travail", String.valueOf(valeur(((Map<String, Object>) ligne.get("temps")).get("travail"))));
      resume.put("nonConformite", String.valueOf(valeur(((Map<String, Object>) ligne.get("temps")).get("nonConformite"))));
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
    return new java.math.BigDecimal(String.valueOf(valeur(valeur))).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private static Object valeur(Object total) {
    return total instanceof Map<?, ?> map ? map.get("valeur") : total;
  }

  private static String id() {
    return (String) CucumberRestTestContext.getElement("$.id");
  }
}

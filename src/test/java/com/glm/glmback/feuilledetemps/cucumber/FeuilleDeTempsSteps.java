package com.glm.glmback.feuilledetemps.cucumber;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.CategoriesDeProduitDesScenarios;
import com.glm.glmback.cucumber.CucumberClock;
import com.glm.glmback.cucumber.EcrituresDuJournalDAtelier;
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
  private static final String FEUILLES_URI = "/api/feuilles-de-temps";
  private static final String ELEMENTS_URI = "/api/elements-de-fabrication";
  private static final String SUIVIS_URI = "/api/atelier/suivis";
  private static final String POSTES_URI = "/api/postes-de-travail";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  @Autowired
  private EcrituresDuJournalDAtelier ecritures;

  private final Map<String, String> operateurs = new HashMap<>();
  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> elements = new HashMap<>();
  private final Map<String, String> suivis = new HashMap<>();
  private final Map<String, String> pointages = new HashMap<>();
  private String dernierPointage;

  @Given("la feuille de temps suit l'operateur {string}")
  public void laFeuilleDeTempsSuitLOperateur(String alias) {
    Map<String, Object> corps = Map.of("nom", alias, "prenom", "Feuille " + SEQUENCE.incrementAndGet(), "postes", List.of());
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, String.valueOf(CucumberRestTestContext.getElement("$.id")));
  }

  @Given("la feuille de temps connait le poste {string} de nature {string}")
  public void laFeuilleDeTempsConnaitLePoste(String alias, String nature) {
    rest.post(
      POSTES_URI,
      JSON.writeValueAsString(Map.of("libelle", "Feuille " + alias + " " + SEQUENCE.incrementAndGet(), "nature", nature))
    );
    postes.put(alias, id());
  }

  @Given("la feuille de temps suit l'operateur {string} habilite sur")
  public void laFeuilleDeTempsSuitLOperateurHabiliteSur(String alias, List<String> habilitations) {
    Map<String, Object> corps = Map.of(
      "nom",
      alias,
      "prenom",
      "Feuille " + SEQUENCE.incrementAndGet(),
      "postes",
      habilitations.stream().map(postes::get).toList()
    );
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, id());
  }

  @Given("la feuille de temps connait l'element {string}")
  public void laFeuilleDeTempsConnaitLElement(String alias) {
    CategoriesDeProduitDesScenarios.declarer(rest, "OF");
    Map<String, Object> element = Map.of("categorie", "OF", "reference", "FEUILLE-" + alias + "-" + SEQUENCE.incrementAndGet());
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(element));
    elements.put(alias, id());
  }

  /**
   * Engager un element deja cloture ouvre un nouveau suivi : c'est le reengagement, qui reste le meme element.
   */
  @Given("l'element {string} est engage en atelier a {string}")
  public void lElementEstEngageEnAtelierA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", elements.get(alias))));
    suivis.put(alias, id());
  }

  @Given("{string} pointe {string} sur l'element {string} au poste {string} a {string}")
  public void pointeSurLElementAuPoste(String operateur, String type, String element, String poste, String instant) {
    horloge.ilEst(Instant.parse(instant));
    dernierPointage = UUID.randomUUID().toString();
    Map<String, Object> corps = Map.of(
      "id",
      dernierPointage,
      "type",
      type,
      "operateur",
      operateurs.get(operateur),
      "poste",
      postes.get(poste)
    );
    ecritures.pointe(suivis.get(element), corps);
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("le pointage de l'element doit etre accepte").isTrue();
  }

  @Given("la feuille de temps recoit sur l'element {string} les pointages")
  public void recoitLesPointages(String element, List<Map<String, String>> pointagesRecus) {
    for (Map<String, String> pointage : pointagesRecus) {
      String survenue = pointage.get("survenue");
      horloge.ilEst(Instant.parse(java.util.Optional.ofNullable(pointage.get("reception")).orElse(survenue)));
      dernierPointage = UUID.randomUUID().toString();
      Map<String, Object> corps = new HashMap<>();
      corps.put("id", dernierPointage);
      corps.put("type", pointage.get("type"));
      corps.put("intention", pointage.get("intention"));
      corps.put("operateur", operateurs.get(pointage.get("operateur")));
      corps.put("dateDeSurvenue", survenue);
      if (pointage.containsKey("poste")) {
        corps.put("poste", postes.get(pointage.get("poste")));
      }
      if (pointage.containsKey("cible")) {
        corps.put("cible", pointages.get(pointage.get("cible")));
      }
      if ("REGULARISATION".equals(pointage.get("acte"))) {
        ecritures.regularise(
          suivis.get(element),
          Map.of("id", dernierPointage, "activite", pointages.get(pointage.get("cible")), "dateDeSurvenue", survenue)
        );
      } else {
        ecritures.pointe(suivis.get(element), corps);
      }
      assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("le pointage explicite doit etre accepte").isTrue();
      pointages.put(pointage.get("alias"), dernierPointage);
    }
  }

  @Then("le suivi de la feuille de temps de {string} ne porte aucun conflit")
  public void nePorteAucunConflit(String element) {
    rest.get(SUIVIS_URI + "/" + suivis.get(element));
    assertThat((List<?>) CucumberRestTestContext.getElement("$.conflits")).isEmpty();
  }

  @Given("l'element {string} est cloture a {string}")
  public void lElementEstClotureA(String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.put(SUIVIS_URI + "/" + suivis.get(element) + "/cloture", JSON.writeValueAsString(Map.of("dateDeSurvenue", instant)));
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("la cloture doit etre acceptee").isTrue();
  }

  @When("je consulte la feuille de temps de {string} pour la semaine {int} de {int}")
  public void jeConsulteLaFeuilleDeTempsDe(String alias, int semaine, int annee) {
    consulte(operateurs.get(alias), semaine, annee);
  }

  @When("je consulte la feuille de temps de {string} pour la semaine {int} de {int} avec evaluation {string}")
  public void consulteAvecEvaluation(String alias, int semaine, int annee, String evaluation) {
    rest.get(
      FEUILLES_URI + "/" + operateurs.get(alias) + "?annee=" + annee + "&semaine=" + semaine + "&evaluation={evaluation}",
      Map.of("evaluation", evaluation)
    );
  }

  @Then("la feuille de temps est evaluee a {string}")
  public void estEvalueeA(String evaluation) {
    assertThat(CucumberRestTestContext.getElement("$.evaluation")).isEqualTo(evaluation);
  }

  @Then("la feuille de temps refusee ne porte aucun rapport")
  @SuppressWarnings("unchecked")
  public void nePorteAucunRapport() {
    assertThat((Map<String, Object>) CucumberRestTestContext.getElement("$")).doesNotContainKeys("evaluation", "jours", "operateur");
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

  @Then("la feuille de temps ne porte aucune activite")
  public void nePorteAucuneActivite() {
    assertThat(jours()).allSatisfy(jour -> assertThat((List<?>) jour.get("activites")).isEmpty());
  }

  @Then("la feuille de temps ne porte aucun champ de presence")
  @SuppressWarnings("unchecked")
  public void nePorteAucunChampDePresence() {
    assertThat(jours()).allSatisfy(jour -> {
      assertThat(jour).doesNotContainKey("presence");
      assertThat((List<Map<String, Object>>) jour.get("activites")).allSatisfy(portion -> {
        assertThat(portion).doesNotContainKey("presumee");
        assertThat((Map<String, Object>) portion.get("activite")).doesNotContainKey("presumee");
      });
    });
  }

  /**
   * Chaque activite lue, ses identifiants ramenes aux alias du scenario. Une cellule vide dit que le champ est absent :
   * une activite en cours n'a pas de fin.
   */
  @Then("les activites du {string} sont")
  public void lesActivitesDuSont(String jour, List<Map<String, String>> attendues) {
    List<Map<String, Object>> activites = activitesDu(jour);

    assertThat(activites).hasSameSizeAs(attendues);
    for (int rang = 0; rang < attendues.size(); rang++) {
      Map<String, String> attendue = attendues.get(rang);
      Map<String, Object> lue = activites.get(rang);
      attendue.forEach((cle, valeur) -> assertThat(valeurLue(lue, cle)).as(cle).isEqualTo(attendu(cle, valeur)));
    }
  }

  @Then("les activites a resoudre du {string} sont")
  public void lesActivitesAResoudreDuSont(String jour, List<Map<String, String>> attendues) {
    List<Map<String, Object>> activites = activitesDu(jour);
    assertThat(activites).hasSameSizeAs(attendues);
    for (Map<String, String> attendue : attendues) {
      Map<String, Object> lue = activites
        .stream()
        .filter(activite -> pointages.get(attendue.get("idActivite")).equals(valeurLue(activite, "idActivite")))
        .findFirst()
        .orElseThrow();
      attendue.forEach((cle, valeur) -> assertThat(valeurLue(lue, cle)).as(cle).isEqualTo(attendu(cle, valeur)));
    }
  }

  @Then("le {string} ne porte aucune activite")
  public void neporteAucuneActivite(String jour) {
    assertThat(activitesDu(jour)).isEmpty();
  }

  private Object attendu(String cle, String valeur) {
    return switch (cle) {
      case "element" -> elements.get(valeur);
      case "poste" -> postes.get(valeur);
      case "idActivite" -> pointages.get(valeur);
      default -> valeur;
    };
  }

  @SuppressWarnings("unchecked")
  private static Object valeurLue(Map<String, Object> portion, String cle) {
    Map<String, Object> activite = (Map<String, Object>) portion.get("activite");
    return switch (cle) {
      case "idActivite" -> activite.get("id");
      case "etat" -> activite.get("etat");
      case "debutActivite" -> activite.get("debut");
      case "finActivite" -> activite.get("fin");
      case "finAuPlusTard" -> activite.get("finAuPlusTard");
      default -> portion.get(cle);
    };
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> activitesDu(String jour) {
    return jours()
      .stream()
      .filter(jourDeLaSemaine -> jour.equals(jourDeLaSemaine.get("jour")))
      .findFirst()
      .map(jourDeLaSemaine -> (List<Map<String, Object>>) jourDeLaSemaine.get("activites"))
      .orElseThrow();
  }

  private static String id() {
    return String.valueOf(CucumberRestTestContext.getElement("$.id"));
  }

  private void consulte(String operateur, int semaine, int annee) {
    rest.get(FEUILLES_URI + "/" + operateur + "?annee=" + annee + "&semaine=" + semaine);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> jours() {
    return (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours");
  }
}

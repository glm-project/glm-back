package com.glm.glmback.syntheseheures.cucumber;

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
  private static final String SYNTHESES_URI = "/api/syntheses-des-heures";
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
  private final Map<String, String> pointages = new HashMap<>();
  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> elements = new HashMap<>();
  private final Map<String, String> suivis = new HashMap<>();
  private final Map<String, Map<String, String>> fichesRevisees = new HashMap<>();
  private String dernierPointage;

  @Given("la synthese des heures suit l'operateur {string}")
  public void laSyntheseDesHeuresSuitLOperateur(String alias) {
    Map<String, Object> corps = Map.of("nom", alias, "prenom", "Synthese " + SEQUENCE.incrementAndGet(), "postes", List.of());
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, String.valueOf(CucumberRestTestContext.getElement("$.id")));
  }

  @Given("la synthese des heures connait le poste {string} de nature {string}")
  public void laSyntheseDesHeuresConnaitLePoste(String alias, String nature) {
    rest.post(
      POSTES_URI,
      JSON.writeValueAsString(Map.of("libelle", "Synthese " + alias + " " + SEQUENCE.incrementAndGet(), "nature", nature))
    );
    postes.put(alias, id());
  }

  @Given("la synthese des heures suit l'operateur {string} habilite sur")
  public void laSyntheseDesHeuresSuitLOperateurHabiliteSur(String alias, List<String> habilitations) {
    Map<String, Object> corps = Map.of(
      "nom",
      alias,
      "prenom",
      "Synthese " + SEQUENCE.incrementAndGet(),
      "postes",
      habilitations.stream().map(postes::get).toList()
    );
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, id());
  }

  @Given("la synthese des heures connait l'element {string}")
  public void laSyntheseDesHeuresConnaitLElement(String alias) {
    CategoriesDeProduitDesScenarios.declarer(rest, "MOULE");
    Map<String, Object> element = Map.of("categorie", "MOULE", "reference", "SYNTHESE-" + alias + "-" + SEQUENCE.incrementAndGet());
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(element));
    elements.put(alias, id());
  }

  /**
   * La fiche a ete creee a l'heure reelle, en debut de scenario : sa revision ne peut pas la preceder. La reference,
   * unique dans l'entreprise, recoit un suffixe propre au scenario.
   */
  @Given("la fiche de l'element {string} est revisee avec la reference {string} et la description {string}")
  public void laFicheDeLElementEstRevisee(String alias, String reference, String description) {
    horloge.ilEst(Instant.now());
    String unique = reference + "-" + SEQUENCE.incrementAndGet();
    rest.put(ELEMENTS_URI + "/" + elements.get(alias), JSON.writeValueAsString(Map.of("reference", unique, "description", description)));
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("la revision de la fiche doit etre acceptee").isTrue();
    fichesRevisees.put(alias, Map.of("reference", unique, "description", description));
  }

  /**
   * Engager un element deja cloture ouvre un nouveau suivi : c'est le reengagement, qui reste le meme element.
   */
  @Given("pour la synthese, l'element {string} est engage en atelier a {string}")
  public void pourLaSyntheseLElementEstEngageA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", elements.get(alias))));
    suivis.put(alias, id());
  }

  @Given("{string} enregistre {string} sur l'element {string} au poste {string} a {string}")
  public void enregistreSurLElementAuPoste(String operateur, String type, String element, String poste, String instant) {
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

  @Given("la synthese des heures recoit sur l'element {string} les pointages")
  public void recoitLesPointages(String element, List<Map<String, String>> pointagesRecus) {
    for (Map<String, String> pointage : pointagesRecus) {
      String survenue = pointage.get("survenue");
      horloge.ilEst(Instant.parse(java.util.Optional.ofNullable(pointage.get("reception")).orElse(survenue)));
      dernierPointage = UUID.randomUUID().toString();
      Map<String, Object> corps = new HashMap<>();
      corps.put("id", dernierPointage);
      corps.put("type", pointage.get("type"));
      corps.put("operateur", operateurs.get(pointage.get("operateur")));
      corps.put("dateDeSurvenue", survenue);
      if (pointage.containsKey("poste")) {
        corps.put("poste", postes.get(pointage.get("poste")));
      }
      if ("REGULARISATION".equals(pointage.get("acte"))) {
        ecritures.regularise(
          suivis.get(element),
          Map.of("id", dernierPointage, "activite", pointages.get(pointage.get("cible")), "dateDeSurvenue", survenue)
        );
      } else {
        ecritures.pointe(suivis.get(element), corps);
      }
      EcrituresDuJournalDAtelier.exigeLaReponseAttendue(pointage);
      pointages.put(pointage.get("alias"), dernierPointage);
    }
  }

  @Given("pour la synthese, l'element {string} est cloture a {string}")
  public void pourLaSyntheseLElementEstClotureA(String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.put(SUIVIS_URI + "/" + suivis.get(element) + "/cloture", JSON.writeValueAsString(Map.of("dateDeSurvenue", instant)));
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("la cloture doit etre acceptee").isTrue();
  }

  @When("je consulte la synthese des heures de {string} pour la semaine {int} de {int}")
  public void jeConsulteLaSyntheseDesHeuresDe(String alias, int semaine, int annee) {
    consulte(operateurs.get(alias), semaine, annee);
  }

  @When("je consulte la synthese des heures de {string} pour la semaine {int} de {int} avec evaluation {string}")
  public void consulteAvecEvaluation(String alias, int semaine, int annee, String evaluation) {
    rest.get(
      SYNTHESES_URI + "/" + operateurs.get(alias) + "?annee=" + annee + "&semaine=" + semaine + "&evaluation={evaluation}",
      Map.of("evaluation", evaluation)
    );
  }

  @Then("la synthese des heures est evaluee a {string}")
  public void estEvalueeA(String evaluation) {
    assertThat(CucumberRestTestContext.getElement("$.evaluation")).isEqualTo(evaluation);
  }

  @Then("la synthese des heures refusee ne porte aucun rapport")
  @SuppressWarnings("unchecked")
  public void nePorteAucunRapport() {
    assertThat((Map<String, Object>) CucumberRestTestContext.getElement("$")).doesNotContainKeys(
      "evaluation",
      "jours",
      "elements",
      "operateur"
    );
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
      assertThat(jour.get("dureeOperationnelle")).isEqualTo(Map.of("valeur", duree));
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

  @Then("le jour {string} a une duree operationnelle de {string}")
  public void leJourAUneDureeOperationnelleDe(String jour, String duree) {
    assertThat(jourDe(jour).get("dureeOperationnelle")).isEqualTo(Map.of("valeur", duree));
  }

  @Then("la duree operationnelle totale de la semaine est {string}")
  public void laDureeOperationnelleTotaleDeLaSemaineEst(String duree) {
    assertThat(CucumberRestTestContext.getElement("$.dureeOperationnelleTotale")).isEqualTo(Map.of("valeur", duree));
  }

  @Then("le journal du {string} est")
  public void leJournalDuEst(String jour, List<Map<String, String>> attendus) {
    compare(pointagesDu(jour), attendus);
  }

  /**
   * Les elements de la semaine, dans l'ordre rendu, chacun reduit aux colonnes du tableau.
   */
  @Then("les elements de la synthese sont")
  public void lesElementsDeLaSyntheseSont(List<Map<String, String>> attendus) {
    compare(elementsDeLaSynthese(), attendus);
  }

  @Then("l'element {string} de la synthese porte les postes")
  public void lElementDeLaSynthesePorteLesPostes(String element, List<Map<String, String>> attendus) {
    List<Map<String, Object>> lus = postesDe(elementDeLaSynthese(element))
      .stream()
      .map(poste -> {
        Map<String, Object> lu = new HashMap<>();
        lu.put("poste", ((Map<?, ?>) poste.get("poste")).get("id"));
        lu.put("nature", poste.get("nature"));
        return lu;
      })
      .toList();

    compare(lus, attendus);
  }

  @Then("l'element {string} de la synthese porte sa fiche revisee")
  public void lElementDeLaSynthesePorteSaFicheRevisee(String element) {
    assertThat(elementDeLaSynthese(element)).containsAllEntriesOf(fichesRevisees.get(element));
  }

  @Then("la synthese ne porte aucun champ de presence ni temps presume")
  @SuppressWarnings("unchecked")
  public void nePorteAucunAncienChamp() {
    Map<String, Object> synthese = (Map<String, Object>) CucumberRestTestContext.getElement("$");
    assertThat(synthese).doesNotContainKeys("dureeTotale", "dureePresumeeTotale", "dureeOperationnellePresumeeTotale");
    assertThat(jours()).allSatisfy(jour -> assertThat(jour).doesNotContainKeys("duree", "dureePresumee", "dureeOperationnellePresumee"));
    assertThat(elementsDeLaSynthese()).allSatisfy(element -> assertThat(element).doesNotContainKey("dureePresumee"));
  }

  private void compare(List<Map<String, Object>> lus, List<Map<String, String>> attendus) {
    assertThat(lus).hasSameSizeAs(attendus);
    for (int rang = 0; rang < attendus.size(); rang++) {
      Map<String, Object> lu = lus.get(rang);
      attendus.get(rang).forEach((cle, valeur) -> assertThat(lu.get(cle)).as(cle).isEqualTo(attendu(cle, valeur)));
    }
  }

  private Object attendu(String cle, String valeur) {
    return switch (cle) {
      case "element", "id" -> elements.get(valeur);
      case "poste" -> postes.get(valeur);
      case "duree", "dureeNonConformite" -> Map.of("valeur", valeur);
      default -> valeur;
    };
  }

  private Map<String, Object> elementDeLaSynthese(String alias) {
    return elementsDeLaSynthese()
      .stream()
      .filter(element -> elements.get(alias).equals(element.get("id")))
      .findFirst()
      .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> elementsDeLaSynthese() {
    return (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.elements");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> postesDe(Map<String, Object> element) {
    return (List<Map<String, Object>>) element.get("postes");
  }

  private static String id() {
    return String.valueOf(CucumberRestTestContext.getElement("$.id"));
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

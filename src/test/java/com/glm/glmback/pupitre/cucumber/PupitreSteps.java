package com.glm.glmback.pupitre.cucumber;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.CucumberClock;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le referentiel du pupitre vu du client HTTP, du pointage a sa relecture.
 *
 * <p>
 * Tout passe par les API : les referentiels pour declarer poste et operateur, l'atelier pour engager, pointer,
 * annuler et cloturer, le pupitre pour relire. C'est ce qui fait de ce scenario la garantie que les deux contextes
 * lisent bien les memes tables — aucun import Java ne relie {@code pupitre} a {@code atelier}.
 * </p>
 *
 * <p>
 * La route ne filtre rien : elle rend tout le referentiel de l'entreprise, que les scenarios precedents ont deja
 * peuple. Les assertions cherchent donc les identites creees par le scenario courant, jamais une egalite de
 * collection.
 * </p>
 */
public class PupitreSteps {

  private static final String POSTES_URI = "/api/postes-de-travail";
  private static final String OPERATEURS_URI = "/api/operateurs";
  private static final String ELEMENTS_URI = "/api/elements-de-fabrication";
  private static final String SUIVIS_URI = "/api/atelier/suivis";
  private static final String JOURNEES_URI = "/api/atelier/journees";
  private static final String REFERENTIEL_URI = "/api/pupitre/referentiel";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> libelles = new HashMap<>();
  private final Map<String, String> operateurs = new HashMap<>();
  private final Map<String, String> matricules = new HashMap<>();
  private final Map<String, String> elements = new HashMap<>();
  private final Map<String, String> references = new HashMap<>();
  private final Map<String, String> suivis = new HashMap<>();
  private String dernierEvenement;

  @Given("le pupitre connait le poste {string}")
  public void lePupitreConnaitLePoste(String alias) {
    String libelle = alias + " " + SEQUENCE.incrementAndGet();
    rest.post(POSTES_URI, JSON.writeValueAsString(Map.of("libelle", libelle, "nature", "Fraisage")));
    postes.put(alias, id());
    libelles.put(alias, libelle);
  }

  @Given("le pupitre connait l'operateur {string} habilite sur")
  public void lePupitreConnaitLOperateur(String alias, List<String> habilitations) {
    String matricule = String.valueOf(900 + SEQUENCE.incrementAndGet());
    Map<String, Object> corps = Map.of(
      "nom",
      alias,
      "prenom",
      "Pupitre " + SEQUENCE.incrementAndGet(),
      "matricule",
      matricule,
      "postes",
      habilitations.stream().map(postes::get).toList()
    );
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, id());
    matricules.put(alias, matricule);
  }

  @Given("le pupitre fabrique {string}")
  public void lePupitreFabrique(String alias) {
    String reference = alias + " " + SEQUENCE.incrementAndGet();
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(Map.of("type", "ORDRE_DE_FABRICATION", "reference", reference)));
    elements.put(alias, id());
    references.put(alias, reference);
  }

  @Given("le pupitre fabrique {string} sans reference")
  public void lePupitreFabriqueSansReference(String alias) {
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(Map.of("type", "PRODUIT")));
    elements.put(alias, id());
  }

  @Given("{string} est engage au pupitre a {string}")
  public void estMisEnAtelierA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", elements.get(alias))));
    suivis.put(alias, id());
  }

  @Given("au pupitre, {string} prend son poste a {string}")
  public void prendSonPosteA(String operateur, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(JOURNEES_URI, JSON.writeValueAsString(Map.of("id", UUID.randomUUID(), "operateur", operateurs.get(operateur))));
  }

  @Given("au pupitre, {string} pointe {string} sur {string} au poste {string} a {string}")
  public void pointeAuPoste(String operateur, String type, String element, String poste, String instant) {
    pointe(
      instant,
      element,
      Map.of("id", UUID.randomUUID(), "type", type, "operateur", operateurs.get(operateur), "poste", postes.get(poste))
    );
  }

  @Given("au pupitre, {string} pointe {string} sur {string} sans poste a {string}")
  public void pointeSansPoste(String operateur, String type, String element, String instant) {
    pointe(instant, element, Map.of("id", UUID.randomUUID(), "type", type, "operateur", operateurs.get(operateur)));
  }

  @Given("le dernier pointage sur {string} est annule a {string}")
  public void leDernierPointageEstAnnule(String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(
      SUIVIS_URI + "/" + suivis.get(element) + "/evenements/" + dernierEvenement + "/annulation",
      JSON.writeValueAsString(Map.of("motif", "saisie en trop"))
    );
  }

  @Given("{string} est supprime du referentiel")
  public void estSupprimeDuReferentiel(String element) {
    rest.delete(ELEMENTS_URI + "/" + elements.get(element));
  }

  @Given("{string} est cloture au pupitre a {string}")
  public void estClotureA(String element, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.put(SUIVIS_URI + "/" + suivis.get(element) + "/cloture", JSON.writeValueAsString(Map.of("dateDeSurvenue", instant)));
  }

  @When("je lis le referentiel du pupitre a {string}")
  public void jeLisLeReferentielDuPupitreA(String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.get(REFERENTIEL_URI);
  }

  @Then("le referentiel du pupitre est date du {string}")
  public void leReferentielEstDateDu(String instant) {
    assertThat(CucumberRestTestContext.getElement("$.genereLe")).isEqualTo(instant);
  }

  @Then("le referentiel du pupitre porte l'operateur {string} avec son matricule")
  public void leReferentielPorteLOperateur(String alias) {
    assertThat(operateur(alias)).containsEntry("nom", alias).containsEntry("matricule", matricules.get(alias));
  }

  @Then("les postes proposes a {string} sont")
  public void lesPostesProposesSont(String alias, List<String> attendus) {
    assertThat(postesDe(alias))
      .extracting(poste -> String.valueOf(poste.get("libelle")))
      .containsExactlyElementsOf(attendus.stream().map(libelles::get).toList());
  }

  @Then("{string} figure au referentiel du pupitre dans l'etat {string}")
  public void figureDansLEtat(String element, String etat) {
    assertThat(suivi(element)).containsEntry("etat", etat);
  }

  @Then("{string} porte au referentiel du pupitre sa reference et son nom d'atelier")
  public void porteSaReferenceEtSonNom(String element) {
    assertThat(suivi(element))
      .containsEntry("reference", references.get(element))
      .containsEntry("type", "ORDRE_DE_FABRICATION")
      .hasEntrySatisfying("nom", nom -> assertThat(String.valueOf(nom)).startsWith("OF-"));
  }

  @Then("{string} ne porte aucune activite au referentiel du pupitre")
  public void nePorteAucuneActivite(String element) {
    assertThat(activites(element)).isEmpty();
  }

  @Then("les activites de {string} au referentiel du pupitre sont")
  public void lesActivitesSont(String element, List<Map<String, String>> attendues) {
    assertThat(resume(element)).isEqualTo(attendues);
  }

  @Then("l'activite de {string} au referentiel du pupitre ne porte aucun poste")
  public void lActiviteNePorteAucunPoste(String element) {
    assertThat(activites(element))
      .singleElement()
      .satisfies(activite -> assertThat(activite).doesNotContainKey("poste"));
  }

  @Then("{string} ne porte aucune reference au referentiel du pupitre")
  public void nePorteAucuneReference(String element) {
    assertThat(suivi(element)).doesNotContainKey("reference");
  }

  @Then("le referentiel du pupitre ne porte aucun element")
  public void nePorteAucunElement() {
    assertThat(suivis()).isEmpty();
  }

  @Then("{string} ne figure pas au referentiel du pupitre")
  public void neFigurePas(String element) {
    assertThat(suivis()).noneSatisfy(suivi -> assertThat(suivi).containsEntry("id", suivis.get(element)));
  }

  @Then("le referentiel du pupitre ne porte ni taux horaire ni cout horaire")
  public void nePorteAucunMontant() {
    assertThat(CucumberRestTestContext.getResponse().orElseThrow()).doesNotContain("tauxHoraire", "coutHoraire");
  }

  private void pointe(String instant, String element, Map<String, Object> corps) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(SUIVIS_URI + "/" + suivis.get(element) + "/pointages", JSON.writeValueAsString(corps));
    List<Map<String, Object>> journal = lus("$.journal");
    dernierEvenement = String.valueOf(journal.getLast().get("id"));
  }

  private List<Map<String, String>> resume(String element) {
    return activites(element)
      .stream()
      .map(activite -> {
        Map<String, String> ligne = new LinkedHashMap<>();
        ligne.put("operateur", alias(operateurs, String.valueOf(activite.get("operateur"))));
        ligne.put("poste", alias(postes, String.valueOf(activite.get("poste"))));
        ligne.put("categorie", String.valueOf(activite.get("categorie")));
        ligne.put("depuis", String.valueOf(activite.get("depuis")));

        return ligne;
      })
      .toList();
  }

  private static String alias(Map<String, String> identites, String identite) {
    return identites
      .entrySet()
      .stream()
      .filter(entree -> entree.getValue().equals(identite))
      .map(Map.Entry::getKey)
      .findFirst()
      .orElse(identite);
  }

  private Map<String, Object> operateur(String alias) {
    return element(lus("$.operateurs"), operateurs.get(alias));
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> postesDe(String alias) {
    return (List<Map<String, Object>>) operateur(alias).get("postes");
  }

  private Map<String, Object> suivi(String element) {
    return element(suivis(), suivis.get(element));
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> activites(String element) {
    return (List<Map<String, Object>>) suivi(element).get("activites");
  }

  private List<Map<String, Object>> suivis() {
    return lus("$.suivis");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> lus(String chemin) {
    return (List<Map<String, Object>>) CucumberRestTestContext.getElement(chemin);
  }

  private static Map<String, Object> element(List<Map<String, Object>> lus, String identite) {
    Optional<Map<String, Object>> trouve = lus
      .stream()
      .filter(lu -> identite.equals(lu.get("id")))
      .findFirst();
    assertThat(trouve).as("identite %s absente du referentiel du pupitre", identite).isPresent();

    return trouve.orElseThrow();
  }

  private static String id() {
    return String.valueOf(CucumberRestTestContext.getElement("$.id"));
  }
}

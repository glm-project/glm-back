package com.glm.glmback.pupitre.cucumber;

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
 * Tout passe par les API : les referentiels pour declarer poste et operateur, l'atelier pour engager, pointer
 * et cloturer, le pupitre pour relire. C'est ce qui fait de ce scenario la garantie que les deux contextes
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
  private static final String REFERENTIEL_URI = "/api/pupitre/referentiel";
  private static final String CATEGORIES_URI = "/api/categories-de-produit";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();
  /**
   * Les scenarios de tous les contextes peuplent le meme schema d'entreprise, et chaque classe de steps porte son
   * propre compteur reparti de zero : {@code cout_de_revient.feature} emploie les memes alias « fraiseuse » et
   * « tour », donc produirait les memes libelles, qui doivent rester uniques. Ce prefixe est ce qui separe les deux.
   */
  private static final String PREFIXE = "pupitre ";

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  @Autowired
  private EcrituresDuJournalDAtelier ecritures;

  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> libelles = new HashMap<>();
  private final Map<String, String> operateurs = new HashMap<>();
  private final Map<String, String> identifiants = new HashMap<>();
  private final Map<String, String> elements = new HashMap<>();
  private final Map<String, String> references = new HashMap<>();
  private final Map<String, String> nomsDAtelier = new HashMap<>();
  private final Map<String, String> suivis = new HashMap<>();
  private final Map<String, String> gestes = new HashMap<>();

  @Given("le pupitre connait le poste {string}")
  public void lePupitreConnaitLePoste(String alias) {
    String libelle = PREFIXE + alias + " " + SEQUENCE.incrementAndGet();
    rest.post(POSTES_URI, JSON.writeValueAsString(Map.of("libelle", libelle, "nature", "Fraisage")));
    postes.put(alias, id());
    libelles.put(alias, libelle);
  }

  @Given("le pupitre connait l'operateur {string} habilite sur")
  public void lePupitreConnaitLOperateur(String alias, List<String> habilitations) {
    String identifiant = String.valueOf(900 + SEQUENCE.incrementAndGet());
    Map<String, Object> corps = Map.of(
      "nom",
      alias,
      "prenom",
      "Pupitre " + SEQUENCE.incrementAndGet(),
      "identifiant",
      identifiant,
      "postes",
      habilitations.stream().map(postes::get).toList()
    );
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, id());
    identifiants.put(alias, identifiant);
  }

  @Given("le pupitre fabrique {string}")
  public void lePupitreFabrique(String alias) {
    String reference = PREFIXE + alias + " " + SEQUENCE.incrementAndGet();
    CategoriesDeProduitDesScenarios.declarer(rest, "OF");
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(Map.of("categorie", "OF", "reference", reference)));
    elements.put(alias, id());
    references.put(alias, reference);
  }

  @Given("le pupitre fabrique {string} sans reference")
  public void lePupitreFabriqueSansReference(String alias) {
    CategoriesDeProduitDesScenarios.declarer(rest, "MOULE");
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(Map.of("categorie", "MOULE")));
    elements.put(alias, id());
  }

  @Given("{string} est engage au pupitre a {string}")
  public void estMisEnAtelierA(String alias, String instant) {
    horloge.ilEst(Instant.parse(instant));
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", elements.get(alias))));
    suivis.put(alias, id());
    nomsDAtelier.put(alias, String.valueOf(CucumberRestTestContext.getElement("$.nom")));
  }

  @Given("la reference de {string} devient {string} au referentiel de fabrication")
  public void changeLaReference(String element, String reference) {
    rest.put(ELEMENTS_URI + "/" + elements.get(element), JSON.writeValueAsString(Map.of("reference", reference)));
    references.put(element, reference);
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

  /**
   * Une ouverture nommee, dont l'identifiant de geste est l'identite de l'activite qu'elle ouvre : c'est lui que
   * visent ensuite une fin ou une transition, et que le referentiel rend dans {@code ouverture}.
   */
  @Given("au pupitre, {string} ouvre {string} en {string} sur {string} au poste {string} a {string}")
  public void ouvre(String operateur, String geste, String type, String element, String poste, String instant) {
    ouvreRecu(operateur, geste, type, element, poste, instant, instant);
  }

  @Given("au pupitre, {string} ouvre {string} en {string} sur {string} au poste {string} a {string}, recu a {string}")
  public void ouvreRecu(String operateur, String geste, String type, String element, String poste, String instant, String recu) {
    envoie(geste, element, recu, geste(operateur, poste, instant, type));
  }

  @Given("au pupitre, {string} termine par {string} sur {string} au poste {string} a {string}")
  public void termine(String operateur, String geste, String element, String poste, String instant) {
    termineRecu(operateur, geste, element, poste, instant, instant);
  }

  @Given("au pupitre, {string} termine par {string} sur {string} au poste {string} a {string}, recu a {string}")
  public void termineRecu(String operateur, String geste, String element, String poste, String instant, String recu) {
    envoie(geste, element, recu, geste(operateur, poste, instant, "FIN"));
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

  @Then("le referentiel du pupitre porte la duree maximale d'activite {string}")
  public void lReferentielPorteLaDureeMaximale(String duree) {
    assertThat(CucumberRestTestContext.getElement("$.dureeMaximaleDActivite")).isEqualTo(duree);
  }

  @Then("les categories du referentiel du pupitre commencent par {string}")
  public void lesCategoriesCommencentPar(String code) {
    assertThat(categories()).startsWith(code);
  }

  /**
   * Relit les categories par la route de gestion : apres ce step, la derniere reponse n'est plus le referentiel.
   */
  @Then("les categories du referentiel du pupitre suivent l'ordre des categories de produit")
  public void lesCategoriesSuiventLOrdreDeLaGestion() {
    List<String> lues = categories();
    rest.get(CATEGORIES_URI + "?size=100");

    assertThat(lues).isEqualTo(CucumberRestTestContext.getElement("$.content..code"));
  }

  @Then("le referentiel du pupitre porte l'operateur {string} avec son identifiant")
  public void leReferentielPorteLOperateur(String alias) {
    assertThat(operateur(alias)).containsEntry("nom", alias).containsEntry("identifiant", identifiants.get(alias));
  }

  @Then("{string} ne porte aucun etat ni echeance de presence au referentiel du pupitre")
  public void nePorteAucunePresence(String alias) {
    assertThat(operateur(alias)).doesNotContainKeys("etat", "presentJusqua");
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
      .containsEntry("categorie", "OF")
      .containsEntry("nom", nomsDAtelier.get(element));
  }

  @Then("{string} ne porte aucune activite au referentiel du pupitre")
  public void nePorteAucuneActivite(String element) {
    assertThat(activites(element)).isEmpty();
  }

  /**
   * Compare les seules colonnes du tableau : operateur, poste et ouverture se lisent par leur nom de scenario, les
   * autres telles que la reponse les porte.
   */
  @Then("les activites de {string} au referentiel du pupitre sont")
  public void lesActivitesSont(String element, List<Map<String, String>> attendues) {
    assertThat(resume(element, attendues.getFirst().keySet())).isEqualTo(attendues);
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

  private Map<String, Object> geste(String operateur, String poste, String instant, String type) {
    Map<String, Object> corps = new LinkedHashMap<>();
    corps.put("id", UUID.randomUUID().toString());
    corps.put("type", type);
    corps.put("operateur", operateurs.get(operateur));
    if (poste != null) {
      corps.put("poste", postes.get(poste));
    }
    corps.put("dateDeSurvenue", instant);

    return corps;
  }

  private void envoie(String geste, String element, String recu, Map<String, Object> corps) {
    horloge.ilEst(Instant.parse(recu));
    ecritures.pointe(suivis.get(element), corps);
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("pointage de %s", geste).isTrue();
    gestes.put(geste, String.valueOf(corps.get("id")));
  }

  private void pointe(String instant, String element, Map<String, Object> corps) {
    horloge.ilEst(Instant.parse(instant));
    ecritures.pointe(suivis.get(element), corps);
    assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("le pointage doit etre accepte").isTrue();
  }

  private List<Map<String, String>> resume(String element, Iterable<String> colonnes) {
    return activites(element)
      .stream()
      .map(activite -> {
        Map<String, String> ligne = new LinkedHashMap<>();
        colonnes.forEach(colonne -> ligne.put(colonne, lu(colonne, activite.get(colonne))));

        return ligne;
      })
      .toList();
  }

  private String lu(String colonne, Object valeur) {
    return switch (colonne) {
      case "operateur" -> alias(operateurs, String.valueOf(valeur));
      case "poste" -> alias(postes, String.valueOf(valeur));
      case "ouverture" -> alias(gestes, String.valueOf(valeur));
      default -> String.valueOf(valeur);
    };
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

  @SuppressWarnings("unchecked")
  private static List<String> categories() {
    return (List<String>) CucumberRestTestContext.getElement("$.categories");
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

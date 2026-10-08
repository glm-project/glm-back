package com.glm.glmback.atelier.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.CategoriesDeProduitDesScenarios;
import com.glm.glmback.cucumber.CucumberClock;
import com.glm.glmback.cucumber.EcrituresDuJournalDAtelier;
import com.glm.glmback.cucumber.EcrituresDuJournalDAtelier.PointageEnvoye;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Les gestes de l'atelier vus du client HTTP.
 *
 * <p>
 * Les identifiants ne sont jamais ecrits dans les features : ils sont relus de la reponse precedente et retenus sous
 * un nom metier ("l'OF 42"), ce qui laisse les scenarios raconter la journee plutot que des UUID.
 * </p>
 */
public class AtelierSteps {

  private static final String SUIVIS_URI = "/api/atelier/suivis";
  private static final String ELEMENTS_URI = "/api/elements-de-fabrication";
  private static final String POSTES_URI = "/api/postes-de-travail";
  private static final String OPERATEURS_URI = "/api/operateurs";
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  /**
   * Les postes sont declares une fois pour toute la campagne : leur libelle est unique par entreprise, et le garder
   * litteral laisse les scenarios assurer leurs attentes sur « fraiseuse-1 » plutot que sur un identifiant.
   */
  private static final Map<String, String> POSTES_DECLARES = new ConcurrentHashMap<>();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private CucumberClock horloge;

  @Autowired
  private EcrituresDuJournalDAtelier ecritures;

  private final Map<String, String> elements = new HashMap<>();
  private Map<String, Object> suiviSansJournal;

  private final Map<String, String> suivis = new HashMap<>();
  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> operateurs = new HashMap<>();

  private String dernierGesteUri;
  private String dernierGesteCorps;

  @Given("il est {string}")
  public void ilEst(String instant) {
    horloge.ilEst(Instant.parse(instant));
  }

  /**
   * Le referentiel se declare sous un nom metier, mais avec une identite rendue unique : deux scenarios peuvent ainsi
   * parler du meme « dupont » sans se heurter a l'unicite de l'identite dans l'entreprise.
   */
  @Given("l'entreprise a declare le poste de travail {string} de nature {string}")
  public void lEntrepriseADeclareLePosteDeTravail(String alias, String nature) {
    declareLePosteDeTravail(alias, nature, null);
  }

  @Given("l'entreprise a declare le poste de travail {string} de nature {string} et de cout horaire {string}")
  public void lEntrepriseADeclareLePosteDeTravailAvecCoutHoraire(String alias, String nature, String coutHoraire) {
    declareLePosteDeTravail(alias, nature, coutHoraire);
  }

  @Given("l'entreprise a declare l'operateur {string} habilite sur {string}")
  public void lEntrepriseADeclareLOperateurHabilite(String alias, String poste) {
    declareLOperateur(alias, List.of(postes.get(poste)), null);
  }

  @Given("l'entreprise a declare l'operateur {string} habilite sur {string} et {string}")
  public void lEntrepriseADeclareLOperateurHabiliteSurDeuxPostes(String alias, String premier, String second) {
    declareLOperateur(alias, List.of(postes.get(premier), postes.get(second)), null);
  }

  @Given("l'entreprise a declare l'operateur {string} habilite sur {string} et {string} avec un taux horaire de {string}")
  public void lEntrepriseADeclareLOperateurHabiliteSurDeuxPostesAvecTauxHoraire(
    String alias,
    String premier,
    String second,
    String tauxHoraire
  ) {
    declareLOperateur(alias, List.of(postes.get(premier), postes.get(second)), tauxHoraire);
  }

  @Given("l'entreprise a declare l'operateur {string}")
  public void lEntrepriseADeclareLOperateur(String alias) {
    declareLOperateur(alias, List.of(), null);
  }

  @Given("l'entreprise a declare l'operateur {string} sans habilitation")
  public void lEntrepriseADeclareLOperateurSansHabilitation(String alias) {
    declareLOperateur(alias, List.of(), null);
  }

  @Given("l'entreprise a cree l'element de fabrication {string}")
  public void lEntrepriseACreeLElementDeFabrication(String alias, Map<String, String> donnees) {
    CategoriesDeProduitDesScenarios.declarer(rest, donnees.get("categorie"));
    rest.post(ELEMENTS_URI, JSON.writeValueAsString(donnees));
    elements.put(alias, idDeLaDerniereReponse());
  }

  @When("j'engage l'element {string} en atelier")
  public void jEngageLElementEnAtelier(String alias) {
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", elements.get(alias))));
  }

  @Given("j'ai engage l'element {string} en atelier")
  public void jaiEngageLElementEnAtelier(String alias) {
    jEngageLElementEnAtelier(alias);
    suivis.put(alias, idDeLaDerniereReponse());
  }

  @When("j'engage l'element inconnu {string} en atelier")
  public void jEngageLElementInconnu(String id) {
    rest.post(SUIVIS_URI, JSON.writeValueAsString(Map.of("element", id)));
  }

  @When("je pointe sur {string}")
  public void jePointeSur(String alias, Map<String, String> donnees) {
    PointageEnvoye pointage = ecritures.pointe(suivis.get(alias), resoluAvecIdentifiant(donnees));
    dernierGesteUri = pointage.uri();
    dernierGesteCorps = pointage.corps();
  }

  @Given("j'ai pointe sur {string}")
  public void jaiPointeSur(String alias, Map<String, String> donnees) {
    jePointeSur(alias, donnees);
  }

  @When("je pointe sur {string} sans intention")
  public void jePointeSurSansIntention(String alias, Map<String, String> donnees) {
    ecritures.pointeTelQuel(suivis.get(alias), resoluAvecIdentifiant(donnees));
  }

  /**
   * Regularise la fin de l'activite donnee par son identifiant : le corps ne porte que l'activite et l'heure du fait.
   */
  @When("je regularise sur {string}")
  public void jeRegulariseSur(String alias, Map<String, String> donnees) {
    PointageEnvoye regularisation = ecritures.regularise(suivis.get(alias), donnees);
    dernierGesteUri = regularisation.uri();
    dernierGesteCorps = regularisation.corps();
  }

  @When("je regularise sur {string} sans identifiant de saisie")
  public void jeRegulariseSurSansIdentifiant(String alias, Map<String, String> donnees) {
    ecritures.regulariseTelQuel(suivis.get(alias), donnees);
  }

  /**
   * Regularise la fin de l'activite qu'ouvre l'evenement de ce rang du journal.
   */
  @When("je regularise sur {string} en visant l'activite de l'evenement {int}")
  public void jeRegulariseSurLActiviteDe(String alias, int ouvrant, Map<String, String> donnees) {
    jeRegulariseSur(alias, visant(alias, ouvrant, donnees));
  }

  @When("je renvoie la derniere regularisation")
  public void jeRenvoieLaDerniereRegularisation() {
    rest.post(dernierGesteUri, dernierGesteCorps);
  }

  @When("je cloture {string}")
  public void jeCloture(String alias, Map<String, String> donnees) {
    rest.put(SUIVIS_URI + "/" + suivis.get(alias) + "/cloture", JSON.writeValueAsString(donnees));
  }

  @Given("j'ai cloture {string}")
  public void jaiCloture(String alias, Map<String, String> donnees) {
    jeCloture(alias, donnees);
  }

  @When("je cloture {string} a l'instant present")
  public void jeClotureALInstantPresent(String alias) {
    rest.put(SUIVIS_URI + "/" + suivis.get(alias) + "/cloture", "{}");
  }

  @When("je rouvre {string}")
  public void jeRouvre(String alias) {
    rest.delete(SUIVIS_URI + "/" + suivis.get(alias) + "/cloture");
  }

  @When("je consulte {string}")
  public void jeConsulte(String alias) {
    rest.get(SUIVIS_URI + "/" + suivis.get(alias));
  }

  @When("je consulte le suivi inconnu {string}")
  public void jeConsulteLeSuiviInconnu(String id) {
    rest.get(SUIVIS_URI + "/" + id);
  }

  @When("je tente de supprimer le poste de travail declare {string}")
  public void jeTenteDeSupprimerLePosteDeTravailDeclare(String alias) {
    rest.delete(POSTES_URI + "/" + postes.get(alias));
  }

  /**
   * Retire l'habilitation sans toucher au journal d'atelier : c'est ce qui laisse un test distinguer le refus par
   * habilitation du refus, definitif, par pointage deja effectue.
   */
  @Given("l'operateur {string} n'est plus habilite sur {string}")
  @SuppressWarnings("unchecked")
  public void lOperateurNEstPlusHabiliteSur(String alias, String poste) {
    rest.get(OPERATEURS_URI + "/" + operateurs.get(alias));
    String nom = (String) CucumberRestTestContext.getElement("$.nom");
    String prenom = (String) CucumberRestTestContext.getElement("$.prenom");
    List<String> restants = ((List<String>) CucumberRestTestContext.getElement("$.postes[*].id")).stream()
      .filter(id -> !id.equals(postes.get(poste)))
      .toList();

    rest.put(
      OPERATEURS_URI + "/" + operateurs.get(alias),
      JSON.writeValueAsString(Map.of("nom", nom, "prenom", prenom, "postes", restants))
    );
  }

  @When("je tente de supprimer l'operateur declare {string}")
  public void jeTenteDeSupprimerLOperateurDeclare(String alias) {
    rest.delete(OPERATEURS_URI + "/" + operateurs.get(alias));
  }

  @When("je liste les elements engages")
  public void jeListeLesElementsEngages() {
    rest.get(SUIVIS_URI);
  }

  @When("je liste les elements engages dans l'etat {string}")
  public void jeListeLesElementsEngagesDansLEtat(String etat) {
    rest.get(SUIVIS_URI + "?etats=" + etat);
  }

  @When("je liste les elements engages entre {string} et {string}")
  public void jeListeLesElementsEngagesEntre(String debut, String fin) {
    rest.get(SUIVIS_URI + "?debut=" + debut + "&fin=" + fin);
  }

  @When("je liste les elements engages dans l'etat {string} entre {string} et {string}")
  public void jeListeLesElementsEngagesDansLEtatEntre(String etat, String debut, String fin) {
    rest.get(SUIVIS_URI + "?etats=" + etat + "&debut=" + debut + "&fin=" + fin);
  }

  @When("je liste les elements engages depuis {string} sans borne de fin")
  public void jeListeLesElementsEngagesDepuis(String debut) {
    rest.get(SUIVIS_URI + "?debut=" + debut);
  }

  @Then("le suivi a l'etat {string}")
  public void leSuiviALEtat(String etat) {
    assertThatLastResponse().hasElement("$.etat").withValue(etat);
  }

  @Then("le journal du suivi contient {int} evenements")
  public void leJournalDuSuiviContient(int count) {
    assertThatLastResponse().hasElement("$.journal").withElementsCount(count);
  }

  @Then("le journal du suivi ne contient que les types")
  public void leJournalDuSuiviNeContientQueLesTypes(List<String> types) {
    assertThat(typesDuJournal()).containsExactlyElementsOf(types);
  }

  @Then("le suivi a {int} activites en cours")
  public void leSuiviAActivitesEnCours(int count) {
    assertThatLastResponse().hasElement("$.activitesEnCours").withElementsCount(count);
  }

  @Then("l'activite en cours est de categorie {string}")
  public void lActiviteEnCoursEstDeCategorie(String categorie) {
    assertThatLastResponse().hasElement("$.activitesEnCours[0].categorie").withValue(categorie);
  }

  @Then("l'activite en cours est de categorie {string} depuis {string}")
  public void lActiviteEnCoursEstDeCategorieDepuis(String categorie, String depuis) {
    assertThatLastResponse()
      .hasElement("$.activitesEnCours[0].categorie")
      .withValue(categorie)
      .and()
      .hasElement("$.activitesEnCours[0].depuis")
      .withValue(depuis);
  }

  @Then("les activites en cours sont")
  public void lesActivitesEnCoursSont(List<Map<String, String>> attendues) {
    assertThatLastResponse().hasElement("$.activitesEnCours").containingExactly(attendues);
  }

  @Then("l'evenement {int} du suivi n'ouvre aucune activite")
  public void lEvenementDuSuiviNOuvreAucuneActivite(int rang) {
    assertThat(CucumberRestTestContext.getElement("$.journal[" + rang + "].activite")).isNull();
  }

  @Then("l'evenement {int} du suivi est une regularisation de {string} saisie par {string}")
  public void lEvenementDuSuiviEstUneRegularisation(int rang, String operateur, String saisiPar) {
    assertThatLastResponse()
      .hasElement("$.journal[" + rang + "].estUneRegularisation")
      .withValue(true)
      .and()
      .hasElement("$.journal[" + rang + "].operateur.id")
      .withValue(idDeLOperateur(operateur))
      .and()
      .hasElement("$.journal[" + rang + "].auteur")
      .withValue(saisiPar);
  }

  @Then("l'evenement {int} du suivi n'est pas une regularisation")
  public void lEvenementDuSuiviNEstPasUneRegularisation(int rang) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].estUneRegularisation").withValue(false);
  }

  @Then("l'evenement {int} du suivi porte l'operateur {string} et le poste {string}")
  public void lEvenementDuSuiviPorteLeReferentiel(int rang, String operateur, String poste) {
    assertThatLastResponse()
      .hasElement("$.journal[" + rang + "].operateur.id")
      .withValue(idDeLOperateur(operateur))
      .and()
      .hasElement("$.journal[" + rang + "].poste.id")
      .withValue(postes.get(poste));
  }

  @Then("l'evenement {int} du suivi a la nature {string}")
  public void lEvenementDuSuiviALaNature(int rang, String nature) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].nature").withValue(nature);
  }

  @Then("l'evenement {int} du suivi a le cout horaire {string}")
  public void lEvenementDuSuiviALeCoutHoraire(int rang, String coutHoraire) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].coutHoraire").withValue(coutHoraire);
  }

  @Then("l'evenement {int} du suivi a le taux horaire {string}")
  public void lEvenementDuSuiviALeTauxHoraire(int rang, String tauxHoraire) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].tauxHoraire").withValue(tauxHoraire);
  }

  @Then("je retiens les informations du suivi hors journal")
  @SuppressWarnings("unchecked")
  public void jeRetiensLesInformationsDuSuiviHorsJournal() {
    suiviSansJournal = new HashMap<>((Map<String, Object>) CucumberRestTestContext.getElement("$"));
    suiviSansJournal.remove("journal");
  }

  @Then("la grille contient les memes informations sans journal")
  public void laGrilleContientLesMemesInformationsSansJournal() {
    assertThatLastResponse()
      .hasOkStatus()
      .hasElement("$.content[?(@.id == '" + suiviSansJournal.get("id") + "')]")
      .withValue(List.of(suiviSansJournal));
    assertThatLastResponse().hasElement("$.content[*].journal").withElementsCount(0);
  }

  @Then("la liste des elements engages contient {int} elements")
  public void laListeDesElementsEngagesContient(int count) {
    assertThatLastResponse().hasElement("$.content").withElementsCount(count);
  }

  @Then("la liste des elements engages contient {string}")
  public void laListeDesElementsEngagesContientLElement(String alias) {
    assertThat(identifiantsDeLaListe()).contains(suivis.get(alias));
  }

  @Then("la liste des elements engages ne contient pas {string}")
  public void laListeDesElementsEngagesNeContientPasLElement(String alias) {
    assertThat(identifiantsDeLaListe()).doesNotContain(suivis.get(alias));
  }

  @Then("la liste des elements engages contient au moins {int} elements")
  public void laListeDesElementsEngagesContientAuMoins(int count) {
    assertThatLastResponse().hasElement("$.content").withMoreThanElementsCount(count);
  }

  @When("je rejoue le dernier geste du pupitre")
  public void jeRejoueLeDernierGesteDuPupitre() {
    rest.post(dernierGesteUri, dernierGesteCorps);
  }

  @When("je pointe sur {string} sans identifiant de geste")
  public void jePointeSansIdentifiant(String alias, Map<String, String> donnees) {
    ecritures.pointe(suivis.get(alias), resolu(donnees));
  }

  @Then("l'evenement {int} du suivi a l'identifiant {string}")
  public void lEvenementDuSuiviALIdentifiant(int rang, String id) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].id").withValue(id);
  }

  @Then("l'evenement {int} du suivi a survenu a {string} et a ete saisi a {string} par {string}")
  public void lEvenementDuSuiviEstBitemporel(int rang, String survenue, String enregistrement, String auteur) {
    assertThatLastResponse()
      .hasElement("$.journal[" + rang + "].dateDeSurvenue")
      .withValue(survenue)
      .and()
      .hasElement("$.journal[" + rang + "].dateDEnregistrement")
      .withValue(enregistrement)
      .and()
      .hasElement("$.journal[" + rang + "].auteur")
      .withValue(auteur);
  }

  @SuppressWarnings("unchecked")
  private static List<String> identifiantsDeLaListe() {
    return (List<String>) CucumberRestTestContext.getElement("$.content[*].id");
  }

  @SuppressWarnings("unchecked")
  private static List<String> typesDuJournal() {
    return (List<String>) CucumberRestTestContext.getElement("$.journal[*].type");
  }

  private String evenementDAtelier(String alias, int rang) {
    rest.get(SUIVIS_URI + "/" + suivis.get(alias));

    return elementDeLaDerniereReponse("$.journal[" + rang + "].id");
  }

  /**
   * Traduit les noms metier des features en identifiants du referentiel. Une valeur inconnue passe telle quelle : les
   * scenarios de refus fournissent directement un identifiant qui n'existe pas.
   */
  private Map<String, String> resolu(Map<String, String> donnees) {
    Map<String, String> corps = new HashMap<>(donnees);
    corps.computeIfPresent("operateur", (cle, alias) -> operateurs.getOrDefault(alias, alias));
    corps.computeIfPresent("poste", (cle, alias) -> postes.getOrDefault(alias, alias));

    return corps;
  }

  private Map<String, String> resoluAvecIdentifiant(Map<String, String> donnees) {
    Map<String, String> corps = resolu(donnees);
    corps.putIfAbsent("id", UUID.randomUUID().toString());
    return corps;
  }

  /**
   * Le corps donne, dont l'activite est celle qu'ouvre l'evenement de ce rang du journal.
   */
  private Map<String, String> visant(String alias, int ouvrant, Map<String, String> donnees) {
    rest.get(SUIVIS_URI + "/" + suivis.get(alias));
    Map<String, String> corps = new HashMap<>(donnees);
    corps.put("activite", elementDeLaDerniereReponse("$.journal[" + ouvrant + "].activite"));

    return corps;
  }

  private String idDeLOperateur(String alias) {
    return operateurs.getOrDefault(alias, alias);
  }

  private void declareLePosteDeTravail(String alias, String nature, String coutHoraire) {
    postes.put(
      alias,
      POSTES_DECLARES.computeIfAbsent(alias, libelle -> {
        Map<String, Object> corps = new HashMap<>(Map.of("libelle", libelle, "nature", nature));
        if (coutHoraire != null) {
          corps.put("coutHoraire", coutHoraire);
        }
        rest.post(POSTES_URI, JSON.writeValueAsString(corps));

        return idDeLaDerniereReponse();
      })
    );
  }

  private void declareLOperateur(String alias, List<String> habilitations, String tauxHoraire) {
    Map<String, Object> corps = new HashMap<>(
      Map.of("nom", alias, "prenom", "Operateur " + SEQUENCE.incrementAndGet(), "postes", habilitations)
    );
    if (tauxHoraire != null) {
      corps.put("tauxHoraire", tauxHoraire);
    }
    rest.post(OPERATEURS_URI, JSON.writeValueAsString(corps));
    operateurs.put(alias, idDeLaDerniereReponse());
  }

  private static String idDeLaDerniereReponse() {
    return elementDeLaDerniereReponse("$.id");
  }

  private static String elementDeLaDerniereReponse(String jsonPath) {
    return (String) CucumberRestTestContext.getElement(jsonPath);
  }
}

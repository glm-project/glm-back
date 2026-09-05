package com.glm.glmback.atelier.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
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
  private static final String JOURNEES_URI = "/api/atelier/journees";
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

  private final Map<String, String> elements = new HashMap<>();
  private final Map<String, String> suivis = new HashMap<>();
  private final Map<String, String> postes = new HashMap<>();
  private final Map<String, String> operateurs = new HashMap<>();

  private String derniereJournee;

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
    rest.post(SUIVIS_URI + "/" + suivis.get(alias) + "/pointages", JSON.writeValueAsString(resoluAvecIdentifiant(donnees)));
  }

  @Given("j'ai pointe sur {string}")
  public void jaiPointeSur(String alias, Map<String, String> donnees) {
    jePointeSur(alias, donnees);
  }

  @When("je regularise sur {string}")
  public void jeRegulariseSur(String alias, Map<String, String> donnees) {
    rest.post(SUIVIS_URI + "/" + suivis.get(alias) + "/regularisations", JSON.writeValueAsString(resolu(donnees)));
  }

  @When("j'annule l'evenement {int} de {string}")
  public void jAnnuleLEvenementDe(int rang, String alias, Map<String, String> donnees) {
    rest.post(
      SUIVIS_URI + "/" + suivis.get(alias) + "/evenements/" + evenementDAtelier(alias, rang) + "/annulation",
      JSON.writeValueAsString(donnees)
    );
  }

  @When("j'annule l'evenement inconnu {string} de {string}")
  public void jAnnuleLEvenementInconnuDe(String evenement, String alias, Map<String, String> donnees) {
    rest.post(SUIVIS_URI + "/" + suivis.get(alias) + "/evenements/" + evenement + "/annulation", JSON.writeValueAsString(donnees));
  }

  @When("je corrige l'evenement {int} de {string}")
  public void jeCorrigeLEvenementDe(int rang, String alias, Map<String, String> donnees) {
    rest.put(
      SUIVIS_URI + "/" + suivis.get(alias) + "/evenements/" + evenementDAtelier(alias, rang),
      JSON.writeValueAsString(resolu(donnees))
    );
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

  @When("je consulte le temps effectif de {string}")
  public void jeConsulteLeTempsEffectifDe(String alias) {
    rest.get(SUIVIS_URI + "/" + suivis.get(alias) + "/temps-effectif");
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

  @Then("l'evenement {int} du suivi est annule avec le motif {string}")
  public void lEvenementDuSuiviEstAnnuleAvecLeMotif(int rang, String motif) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].annulation.motif").withValue(motif);
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

  @Then("le temps effectif contient")
  public void leTempsEffectifContient(List<Map<String, String>> attendus) {
    assertThatLastResponse().hasResponse().containingExactly(attendus);
  }

  @Then("le temps effectif ne contient aucun intervalle ouvert")
  public void leTempsEffectifNeContientAucunIntervalleOuvert() {
    assertThat(CucumberRestTestContext.countEntries("$[?(!@.fin)]")).isZero();
  }

  @Then("la liste des elements engages contient {int} elements")
  public void laListeDesElementsEngagesContient(int count) {
    assertThatLastResponse().hasElement("$.content").withElementsCount(count);
  }

  @Then("la liste des elements engages contient au moins {int} elements")
  public void laListeDesElementsEngagesContientAuMoins(int count) {
    assertThatLastResponse().hasElement("$.content").withMoreThanElementsCount(count);
  }

  @When("j'arrive")
  public void jArrive(Map<String, String> donnees) {
    rest.post(JOURNEES_URI, JSON.writeValueAsString(resoluAvecIdentifiant(donnees)));
  }

  @Given("je suis arrive")
  public void jeSuisArrive(Map<String, String> donnees) {
    jArrive(donnees);
    derniereJournee = idDeLaDerniereReponse();
  }

  @When("je pointe ma presence")
  public void jePointeMaPresence(Map<String, String> donnees) {
    rest.post(JOURNEES_URI + "/pointages", JSON.writeValueAsString(resoluAvecIdentifiant(donnees)));
  }

  @Given("j'ai pointe ma presence")
  public void jaiPointeMaPresence(Map<String, String> donnees) {
    jePointeMaPresence(donnees);
  }

  @When("je regularise ma journee")
  public void jeRegulariseMaJournee(Map<String, String> donnees) {
    rest.post(JOURNEES_URI + "/" + derniereJournee + "/regularisations", JSON.writeValueAsString(donnees));
  }

  @Given("j'ai regularise ma journee")
  public void jaiRegulariseMaJournee(Map<String, String> donnees) {
    jeRegulariseMaJournee(donnees);
  }

  @When("j'annule l'evenement {int} de ma journee")
  public void jAnnuleLEvenementDeMaJournee(int rang, Map<String, String> donnees) {
    rest.post(
      JOURNEES_URI + "/" + derniereJournee + "/evenements/" + evenementDePresence(rang) + "/annulation",
      JSON.writeValueAsString(donnees)
    );
  }

  @When("j'annule l'evenement inconnu {string} de ma journee")
  public void jAnnuleLEvenementInconnuDeMaJournee(String evenement, Map<String, String> donnees) {
    rest.post(JOURNEES_URI + "/" + derniereJournee + "/evenements/" + evenement + "/annulation", JSON.writeValueAsString(donnees));
  }

  @When("je corrige l'evenement {int} de ma journee")
  public void jeCorrigeLEvenementDeMaJournee(int rang, Map<String, String> donnees) {
    rest.put(JOURNEES_URI + "/" + derniereJournee + "/evenements/" + evenementDePresence(rang), JSON.writeValueAsString(donnees));
  }

  @When("je consulte ma journee")
  public void jeConsulteMaJournee() {
    rest.get(JOURNEES_URI + "/" + derniereJournee);
  }

  @When("je consulte la journee inconnue {string}")
  public void jeConsulteLaJourneeInconnue(String id) {
    rest.get(JOURNEES_URI + "/" + id);
  }

  @When("je liste les journees de {string}")
  public void jeListeLesJourneesDe(String operateur) {
    rest.get(JOURNEES_URI + "?operateur=" + idDeLOperateur(operateur));
  }

  @When("je liste les journees de {string} entre {string} et {string}")
  public void jeListeLesJourneesDeEntre(String operateur, String debut, String fin) {
    rest.get(JOURNEES_URI + "?operateur=" + idDeLOperateur(operateur) + "&debut=" + debut + "&fin=" + fin);
  }

  @When("je liste les journees de {string} depuis {string} sans borne de fin")
  public void jeListeLesJourneesDeDepuis(String operateur, String debut) {
    rest.get(JOURNEES_URI + "?operateur=" + idDeLOperateur(operateur) + "&debut=" + debut);
  }

  @When("je liste les journees")
  public void jeListeLesJournees() {
    rest.get(JOURNEES_URI);
  }

  @Then("la journee a l'etat {string}")
  public void laJourneeALEtat(String etat) {
    assertThatLastResponse().hasElement("$.etat").withValue(etat);
  }

  @Then("la journee a l'amplitude de {string} a {string}")
  public void laJourneeALAmplitude(String debut, String fin) {
    assertThatLastResponse().hasElement("$.amplitude.debut").withValue(debut).and().hasElement("$.amplitude.fin").withValue(fin);
  }

  @Then("la journee n'a pas d'amplitude")
  public void laJourneeNAPasDAmplitude() {
    assertThat(CucumberRestTestContext.getElement("$.amplitude")).isNull();
  }

  @Then("les fenetres de presence sont")
  public void lesFenetresDePresenceSont(List<Map<String, String>> attendues) {
    assertThatLastResponse().hasElement("$.fenetres").containingExactly(attendues);
  }

  @Then("le journal de la journee contient {int} evenements")
  public void leJournalDeLaJourneeContient(int count) {
    assertThatLastResponse().hasElement("$.journal").withElementsCount(count);
  }

  @Then("l'evenement {int} de la journee est annule avec le motif {string}")
  public void lEvenementDeLaJourneeEstAnnule(int rang, String motif) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].annulation.motif").withValue(motif);
  }

  @Then("l'evenement {int} de la journee a survenu a {string} et a ete saisi a {string} par {string}")
  public void lEvenementDeLaJourneeEstBitemporel(int rang, String survenue, String enregistrement, String auteur) {
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

  @Then("l'evenement {int} de la journee a l'identifiant {string}")
  public void lEvenementDeLaJourneeALIdentifiant(int rang, String id) {
    assertThatLastResponse().hasElement("$.journal[" + rang + "].id").withValue(id);
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

  @Then("la liste des journees contient {int} journees")
  public void laListeDesJourneesContient(int count) {
    assertThatLastResponse().hasElement("$.content").withElementsCount(count);
  }

  @SuppressWarnings("unchecked")
  private static List<String> typesDuJournal() {
    return (List<String>) CucumberRestTestContext.getElement("$.journal[*].type");
  }

  private String evenementDAtelier(String alias, int rang) {
    rest.get(SUIVIS_URI + "/" + suivis.get(alias));

    return elementDeLaDerniereReponse("$.journal[" + rang + "].id");
  }

  private String evenementDePresence(int rang) {
    rest.get(JOURNEES_URI + "/" + derniereJournee);

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

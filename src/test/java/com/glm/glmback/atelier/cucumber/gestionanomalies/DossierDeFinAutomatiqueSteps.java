package com.glm.glmback.atelier.cucumber.gestionanomalies;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.atelier.cucumber.AtelierSteps;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le parcours d'un dossier d'anomalie ouvert sur une fin automatique : lecture, apercu, confirmation et reprise.
 *
 * <p>
 * Les evenements se designent par leur rang dans le journal du suivi tel qu'il etait avant l'acte : les identifiants
 * d'evenement et d'activite viennent du serveur, jamais d'une valeur supposee.
 * </p>
 */
public class DossierDeFinAutomatiqueSteps {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private AtelierSteps atelier;

  private String suivi;
  private List<Map<String, Object>> journal;
  private String ancre;
  private Map<String, Object> dossier;
  private Map<String, Object> suiviAvant;
  private String commande;
  private Map<String, Object> apercu;
  private Map<String, Object> proposition;
  private Map<String, Object> confirmation;
  private Map<String, Object> suiviApresConfirmation;

  @When("je consulte le dossier d'anomalie de {string} depuis l'evenement {int}")
  @SuppressWarnings("unchecked")
  public void consulte(String alias, int rang) {
    atelier.jeConsulte(alias);
    suiviAvant = (Map<String, Object>) CucumberRestTestContext.getElement("$");
    suivi = (String) suiviAvant.get("id");
    journal = (List<Map<String, Object>>) suiviAvant.get("journal");
    ancre = (String) journal.get(rang).get("id");
    lit(ancre);
  }

  @When("je consulte le dossier d'anomalie de l'ancienne ancre")
  public void consulteLAncienneAncre() {
    lit(ancre);
  }

  @When("je consulte le dossier d'anomalie du remplacant de l'ancre")
  public void consulteLeRemplacant() {
    lit((String) confirmation.get("evenementCree"));
  }

  @When("je consulte le dossier d'anomalie de l'evenement cree par la confirmation")
  public void consulteLEvenementCree() {
    lit((String) confirmation.get("evenementCree"));
  }

  @Then("le dossier d'anomalie est a l'etat {string}")
  public void etat(String etat) {
    assertThat(dossier.get("kind")).isEqualTo(etat);
  }

  @Then("le dossier d'anomalie signale une fin automatique")
  public void signaleUneFinAutomatique() {
    assertThat(dossier.get("finAutomatique")).isEqualTo(true);
  }

  @Then("le dossier d'anomalie ne signale aucune fin automatique")
  public void neSignaleAucuneFinAutomatique() {
    assertThat(dossier.get("finAutomatique")).isEqualTo(false);
  }

  @Then("le dossier d'anomalie declare finAutomatique {string}")
  public void declareFinAutomatique(String valeur) {
    assertThat(dossier.get("finAutomatique")).isEqualTo(Boolean.valueOf(valeur));
  }

  @Then("le dossier d'anomalie est en conflit")
  public void estEnConflit() {
    assertThat(dossier.get("enConflit")).isEqualTo(true);
  }

  @Then("le dossier d'anomalie n'est pas en conflit")
  public void nEstPasEnConflit() {
    assertThat(dossier.get("enConflit")).isEqualTo(false);
  }

  @Then("le dossier d'anomalie donne les activites")
  public void activites(List<Map<String, String>> attendues) {
    var lignes = sansCellulesVides(attendues);
    assertThat(extraites(activitesDe(dossier), lignes)).containsExactlyElementsOf(lignes);
  }

  @Then("le dossier d'anomalie donne {int} activites")
  public void nombreDActivites(int nombre) {
    assertThat(activitesDe(dossier)).hasSize(nombre);
  }

  @Then("le perimetre du dossier d'anomalie contient l'activite de l'evenement {int}")
  @SuppressWarnings("unchecked")
  public void perimetreContient(int rang) {
    var perimetre = (Map<String, Object>) dossier.get("perimetre");
    assertThat((List<Object>) perimetre.get("activites")).contains(journal.get(rang).get("activite"));
  }

  @Then("le dossier d'anomalie propose")
  public void propose(List<Map<String, String>> attendus) {
    var reels = choix().stream().map(this::resume).toList();
    assertThat(
      reels
        .stream()
        .map(reel -> restreint(reel, attendus.getFirst().keySet()))
        .toList()
    ).containsExactlyElementsOf(attendus);
  }

  @Then("le dossier d'anomalie ne propose pas {string}")
  public void nePropose(String code) {
    assertThat(choix()).noneMatch(choix -> code.equals(choix.get("code")));
  }

  @Then("la proposition {string} du dossier d'anomalie reprend l'ouvrant {int} sans aucune heure")
  @SuppressWarnings("unchecked")
  public void reprendLOuvrantSansHeure(String code, int rang) {
    var fait = (Map<String, Object>) choix(code).get("fait");
    var ouvrant = journal.get(rang);
    assertThat(fait).doesNotContainKey("instant");
    assertThat(fait.get("type")).isEqualTo("FIN");
    assertThat(fait.get("intention")).isEqualTo("FIN");
    assertThat(fait.get("activiteVisee")).isEqualTo(ouvrant.get("activite"));
    assertThat(fait.get("operateur")).isEqualTo(ouvrant.get("operateurId"));
    assertThat(fait.get("poste")).isEqualTo(ouvrant.get("posteId"));
    assertThat(choix(code)).doesNotContainKey("motif");
  }

  @Then("la proposition {string} du dossier d'anomalie ne porte aucun poste")
  @SuppressWarnings("unchecked")
  public void propositionSansPoste(String code) {
    assertThat((Map<String, Object>) choix(code).get("fait")).doesNotContainKey("poste");
  }

  @When("je previsualise la proposition {string} du dossier d'anomalie")
  public void previsualiseUneCorrection(String code) {
    apercu(acteDe(code, null));
  }

  @When("je previsualise la proposition {string} du dossier d'anomalie a l'instant {string}")
  public void previsualiseUneRegularisation(String code, String instant) {
    apercu(acteDe(code, instant));
  }

  @When("je previsualise l'acte suivant sur le dossier d'anomalie")
  public void previsualiseUnActe(Map<String, String> donnees) {
    var pointage = journal.get(Integer.parseInt(donnees.get("pointage")));
    var fait = new HashMap<String, Object>();
    fait.put("type", donnees.get("type"));
    fait.put("intention", donnees.get("intention"));
    fait.put("operateur", pointage.get("operateurId"));
    if (pointage.get("posteId") != null) {
      fait.put("poste", pointage.get("posteId"));
    }
    fait.put("instant", donnees.get("instant"));
    var acte = new HashMap<String, Object>();
    acte.put("kind", donnees.get("kind"));
    acte.put("pointage", pointage.get("id"));
    acte.put("motif", donnees.get("motif"));
    acte.put("fait", fait);
    apercu(acte);
  }

  @Then("l'apercu du dossier d'anomalie est accepte")
  public void apercuAccepte() {
    assertThatLastResponse().hasOkStatus();
  }

  @Then("l'apercu du dossier d'anomalie donne les activites")
  public void apercuActivites(List<Map<String, String>> attendues) {
    for (String moment : List.of("avant", "apres")) {
      var sansMoment = sansCellulesVides(attendues)
        .stream()
        .filter(ligne -> moment.equals(ligne.get("moment")))
        .map(ligne -> sans(ligne, "moment"))
        .toList();
      if (!sansMoment.isEmpty()) {
        assertThat(extraites(activitesDe(dossierDe(moment)), sansMoment)).containsExactlyElementsOf(sansMoment);
      }
    }
  }

  @Then("l'apercu du dossier d'anomalie donne apres l'acte un dossier a l'etat {string}")
  public void apercuEtat(String etat) {
    assertThat(dossierDe("apres").get("kind")).isEqualTo(etat);
  }

  @Then("l'apercu du dossier d'anomalie donne apres l'acte un dossier qui n'est pas en conflit")
  public void apercuSansConflit() {
    assertThat(dossierDe("apres").get("enConflit")).isEqualTo(false);
  }

  @Then("l'apercu du dossier d'anomalie donne apres l'acte la raison de conflit {string}")
  @SuppressWarnings("unchecked")
  public void apercuRaison(String raison) {
    var diagnostics = (List<Map<String, Object>>) dossierDe("apres").get("diagnostics");
    assertThat(diagnostics)
      .extracting(diagnostic -> diagnostic.get("raison"))
      .contains(raison);
  }

  @Then("l'acte de l'apercu du dossier d'anomalie ne porte aucun poste")
  @SuppressWarnings("unchecked")
  public void acteSansPoste() {
    var acte = (Map<String, Object>) apercu.get("acte");
    assertThat((Map<String, Object>) acte.get("fait")).doesNotContainKey("poste");
  }

  @Then("l'apercu du dossier d'anomalie ne modifie aucun fait")
  public void apercuNEcritRien() {
    rest.get("/api/atelier/suivis/" + suivi);
    assertThat(CucumberRestTestContext.getElement("$")).isEqualTo(suiviAvant);
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("NON_ATTESTEE");
  }

  @When("je confirme l'apercu du dossier d'anomalie")
  @SuppressWarnings("unchecked")
  public void confirme() {
    confirmer();
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("ENREGISTREE");
    confirmation = new HashMap<>();
    confirmation.put("recu", CucumberRestTestContext.getElement("$.recu"));
    confirmation.put("evenementCree", CucumberRestTestContext.getElement("$.recu.evenementCree"));
    dossier = (Map<String, Object>) CucumberRestTestContext.getElement("$.dossier");
    rest.get("/api/atelier/suivis/" + suivi);
    suiviApresConfirmation = (Map<String, Object>) CucumberRestTestContext.getElement("$");
  }

  @When("je tente de confirmer l'apercu du dossier d'anomalie")
  public void tenteDeConfirmer() {
    confirmer();
  }

  @When("je reprends la confirmation de l'apercu du dossier d'anomalie")
  public void reprend() {
    confirmer();
  }

  @Then("le recu de la reprise est identique et rien n'est ecrit une seconde fois")
  public void repriseIdentique() {
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("ENREGISTREE");
    assertThat(CucumberRestTestContext.getElement("$.recu")).isEqualTo(confirmation.get("recu"));
    rest.get("/api/atelier/suivis/" + suivi);
    assertThat(CucumberRestTestContext.getElement("$")).isEqualTo(suiviApresConfirmation);
  }

  @Then("l'evenement cree par la confirmation est une fin regularisee a {string}")
  public void finRegularisee(String instant) {
    var fait = creeParLaConfirmation();
    assertThat(fait.get("type")).isEqualTo("FIN");
    assertThat(fait.get("intention")).isEqualTo("FIN");
    assertThat(fait.get("estUneRegularisation")).isEqualTo(true);
    assertThat(fait.get("dateDeSurvenue")).isEqualTo(instant);
  }

  @Then("l'evenement cree par la confirmation ne porte aucun poste")
  public void creeSansPoste() {
    assertThat(creeParLaConfirmation().get("posteId")).isNull();
  }

  private void lit(String pointage) {
    rest.get("/api/atelier/suivis/" + suivi + "/anomalies/" + pointage);
    assertThatLastResponse().hasOkStatus();
    dossier = map(CucumberRestTestContext.getElement("$"));
  }

  private void apercu(Map<String, Object> acte) {
    commande = UUID.randomUUID().toString();
    var revision = ((Number) dossier.get("revision")).longValue();
    rest.post(
      "/api/atelier/suivis/" + suivi + "/anomalies/" + ancre + "/apercus",
      JSON.writeValueAsString(Map.of("commande", commande, "revision", revision, "acte", acte))
    );
    if (CucumberRestTestContext.getStatus().is2xxSuccessful()) {
      apercu = map(CucumberRestTestContext.getElement("$"));
      proposition = new HashMap<>();
      for (var champ : List.of("commande", "adresse", "revision", "acte", "empreinteConsequences", "evenement")) {
        var valeur = apercu.get(champ);
        if (valeur != null) {
          proposition.put(champ, valeur);
        }
      }
    }
  }

  private void confirmer() {
    rest.post("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution", JSON.writeValueAsString(proposition));
  }

  private Map<String, Object> acteDe(String code, String instant) {
    var choix = choix(code);
    var fait = new HashMap<>(map(choix.get("fait")));
    if (instant != null) {
      fait.put("instant", instant);
    }
    var acte = new LinkedHashMap<String, Object>();
    acte.put("kind", choix.get("kind"));
    if (!"REGULARISATION".equals(choix.get("kind"))) {
      acte.put("pointage", choix.get("pointage"));
      acte.put("motif", "Fin automatique traitee depuis son dossier");
    }
    acte.put("fait", fait);
    return acte;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> choix() {
    return (List<Map<String, Object>>) dossier.get("choix");
  }

  private Map<String, Object> choix(String code) {
    return choix()
      .stream()
      .filter(choix -> code.equals(choix.get("code")))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Aucune proposition " + code + " dans " + choix()));
  }

  private Map<String, String> resume(Map<String, Object> choix) {
    var resume = new LinkedHashMap<String, String>();
    resume.put("code", (String) choix.get("code"));
    resume.put("kind", (String) choix.get("kind"));
    resume.put("pointage", String.valueOf(rang((String) choix.get("pointage"))));
    var fait = map(choix.get("fait"));
    resume.put("instant", fait.get("instant") == null ? "" : (String) fait.get("instant"));
    return resume;
  }

  private int rang(String pointage) {
    return journal
      .stream()
      .map(fait -> fait.get("id"))
      .toList()
      .indexOf(pointage);
  }

  private Map<String, Object> dossierDe(String moment) {
    return map(apercu.get(moment));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> activitesDe(Map<String, Object> dossier) {
    return (List<Map<String, Object>>) dossier.get("activites");
  }

  /** Les activites du dossier, resumees sur les colonnes attendues ; « evenement » est le rang de l'ouvrant dans le journal. */
  private List<Map<String, String>> extraites(List<Map<String, Object>> activites, List<Map<String, String>> attendues) {
    return activites
      .stream()
      .<Map<String, String>>map(activite -> {
        var resume = new LinkedHashMap<String, String>();
        for (var colonne : attendues.getFirst().keySet()) {
          var valeur = "evenement".equals(colonne)
            ? String.valueOf(rangDeLActivite((String) activite.get("activite")))
            : activite.get(colonne);
          resume.put(colonne, valeur == null ? "" : valeur.toString());
        }
        return resume;
      })
      .toList();
  }

  private int rangDeLActivite(String activite) {
    return journal
      .stream()
      .map(fait -> fait.get("activite"))
      .toList()
      .indexOf(activite);
  }

  private static Map<String, String> restreint(Map<String, String> resume, java.util.Set<String> colonnes) {
    var restreint = new LinkedHashMap<String, String>();
    colonnes.forEach(colonne -> restreint.put(colonne, resume.get(colonne)));
    return restreint;
  }

  /** Une cellule vide d'un tableau Gherkin se lit null ; une activite sans fin ni duree les rend vides. */
  private static List<Map<String, String>> sansCellulesVides(List<Map<String, String>> lignes) {
    return lignes
      .stream()
      .<Map<String, String>>map(ligne -> {
        var lue = new LinkedHashMap<String, String>();
        ligne.forEach((colonne, valeur) -> lue.put(colonne, valeur == null ? "" : valeur));
        return lue;
      })
      .toList();
  }

  private static Map<String, String> sans(Map<String, String> ligne, String colonne) {
    var copie = new LinkedHashMap<>(ligne);
    copie.remove(colonne);
    return copie;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> creeParLaConfirmation() {
    var journalApres = (List<Map<String, Object>>) suiviApresConfirmation.get("journal");
    return journalApres
      .stream()
      .filter(fait -> fait.get("id").equals(confirmation.get("evenementCree")))
      .findFirst()
      .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object valeur) {
    return (Map<String, Object>) valeur;
  }
}

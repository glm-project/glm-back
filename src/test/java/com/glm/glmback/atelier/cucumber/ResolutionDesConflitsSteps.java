package com.glm.glmback.atelier.cucumber;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

public class ResolutionDesConflitsSteps {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private AtelierSteps atelier;

  private String suivi;
  private String element;
  private String commande;
  private String reference;
  private long revision;
  private Map<String, Object> avant;
  private Object tempsAvant;
  private Map<String, Object> apres;
  private Map<String, Object> acte;
  private Object recu;

  @When("je prepare la resolution du conflit de {string} ancre {int}")
  @SuppressWarnings("unchecked")
  public void prepare(String alias, int ancre, Map<String, String> donnees) {
    atelier.jeConsulte(alias);
    avant = (Map<String, Object>) CucumberRestTestContext.getElement("$");
    suivi = (String) avant.get("id");
    element = (String) avant.get("element");
    var journal = (List<Map<String, Object>>) avant.get("journal");
    var pointage = journal.get(Integer.parseInt(donnees.get("pointage")));
    acte = new HashMap<>(Map.of("kind", donnees.get("kind")));
    if (!donnees.get("kind").equals("REGULARISATION")) {
      acte.put("pointage", pointage.get("id"));
      acte.put("motif", donnees.get("motif"));
    }
    if (!donnees.get("kind").equals("ANNULATION")) {
      var fait = new HashMap<String, Object>();
      fait.put("type", donnees.get("type"));
      fait.put("intention", donnees.get("intention"));
      fait.put("operateur", pointage.get("operateurId"));
      if (pointage.get("posteId") != null) {
        fait.put("poste", pointage.get("posteId"));
      }
      fait.put("instant", donnees.get("instant"));
      if (donnees.containsKey("cible") && !donnees.get("cible").isBlank()) {
        fait.put("activiteVisee", journal.get(Integer.parseInt(donnees.get("cible"))).get("activite"));
      }
      acte.put("fait", fait);
    }
    atelier.jeConsulteLeTempsEffectifDe(alias);
    tempsAvant = CucumberRestTestContext.getElement("$");
    rest.get("/api/atelier/suivis/" + suivi + "/conflits/" + journal.get(ancre).get("id"));
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("EN_CONFLIT");
    revision = ((Number) CucumberRestTestContext.getElement("$.revision")).longValue();
    commande = UUID.randomUUID().toString();
    rest.post(
      "/api/atelier/suivis/" + suivi + "/conflits/" + journal.get(ancre).get("id") + "/apercus",
      JSON.writeValueAsString(Map.of("commande", commande, "revision", revision, "acte", acte))
    );
    assertThatLastResponse().hasOkStatus();
    reference = (String) CucumberRestTestContext.getElement("$.reference");
    apres = (Map<String, Object>) CucumberRestTestContext.getElement("$.apres");
    assertThat(CucumberRestTestContext.getElement("$.acte")).isEqualTo(acte);
  }

  @Then("l'apercu donne les activites de resolution")
  public void apercu(List<Map<String, String>> activites) {
    assertThatLastResponse().hasElement("$.apres.activites").containingExactly(activites);
  }

  @Then("la correction conserve l'identite de l'activite ouverte")
  @SuppressWarnings("unchecked")
  public void identiteConservee() {
    var original = ((List<Map<String, Object>>) avant.get("journal")).stream()
      .filter(fait -> fait.get("id").equals(acte.get("pointage")))
      .findFirst()
      .orElseThrow();
    var corriges = (List<Map<String, Object>>) ((Map<String, Object>) apres.get("suivi")).get("journal");
    var remplacement = corriges
      .stream()
      .filter(fait -> original.get("id").equals(fait.get("remplace")))
      .findFirst()
      .orElseThrow();
    assertThat(remplacement.get("activite")).isEqualTo(original.get("activite"));
    assertThat(remplacement.get("id")).isNotEqualTo(original.get("id"));
  }

  @Then("l'apercu ne fixe aucune fin ni duree definitive")
  @SuppressWarnings("unchecked")
  public void sansFinDefinitive() {
    var activites = (List<Map<String, Object>>) apres.get("activites");
    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.get("etat")).isEqualTo("EN_COURS");
        assertThat(activite.get("fin")).isNull();
        assertThat(activite.get("duree")).isNull();
      });
  }

  @Then("l'apercu n'invente aucune activite")
  public void aucuneActivite() {
    assertThatLastResponse().hasElement("$.apres.activites").withElementsCount(0);
  }

  @Then("l'apercu conserve le trou entre {string} et {string}")
  @SuppressWarnings("unchecked")
  public void trou(String fin, String debut) {
    var activites = (List<Map<String, Object>>) apres.get("activites");
    assertThat(activites.get(0).get("fin")).isEqualTo(fin);
    assertThat(activites.get(1).get("debut")).isEqualTo(debut);
  }

  @Then("la lecture ne porte aucune periode pour {string}")
  public void aucunePeriode(String alias) {
    atelier.jeConsulteLeTempsEffectifDe(alias);
    assertThatLastResponse().hasOkStatus().hasElement("$").withElementsCount(0);
  }

  @Then("l'apercu conserve {int} sequence en conflit")
  @SuppressWarnings("unchecked")
  public void conflitRestant(int nombre) {
    var suiviApres = (Map<String, Object>) apres.get("suivi");
    assertThat((List<?>) suiviApres.get("conflits")).hasSize(nombre);
  }

  @Then("la cloture du suivi reste acquise apres cet acte")
  @SuppressWarnings("unchecked")
  public void clotureInchangee() {
    rest.get("/api/atelier/suivis/" + suivi);
    var suiviActuel = (Map<String, Object>) CucumberRestTestContext.getElement("$");
    assertThat(suiviActuel.get("etat")).isEqualTo("CLOTURE");
    assertThat(suiviActuel.get("clotureLe")).isNotNull().isEqualTo(avant.get("clotureLe"));
    assertThat(suiviActuel.get("cloturePar")).isEqualTo(avant.get("cloturePar"));
  }

  @Then("l'apercu ne modifie ni les faits ni les projections ni la revision")
  public void sansEcriture() {
    rest.get("/api/atelier/suivis/" + suivi);
    assertThat(CucumberRestTestContext.getElement("$")).isEqualTo(avant);
    rest.get("/api/atelier/suivis/" + suivi + "/temps-effectif");
    assertThat(CucumberRestTestContext.getElement("$")).isEqualTo(tempsAvant);
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("NON_ATTESTEE");
  }

  @When("je confirme cet apercu de resolution")
  @SuppressWarnings("unchecked")
  public void confirme() {
    rest.post(
      "/api/atelier/suivis/" + suivi + "/confirmations-de-resolution",
      JSON.writeValueAsString(Map.of("commande", commande, "reference", reference))
    );
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("ENREGISTREE");
    recu = CucumberRestTestContext.getElement("$.recu");
    assertThat(CucumberRestTestContext.getElement("$.recu.acte")).isEqualTo(acte);
    assertThat(((Number) CucumberRestTestContext.getElement("$.recu.revisionDeDepart")).longValue()).isEqualTo(revision);
    assertThat(((Number) CucumberRestTestContext.getElement("$.recu.revisionEnregistree")).longValue()).isEqualTo(revision + 1);
    assertThat(CucumberRestTestContext.getElement("$.dossier.activites")).isEqualTo(apres.get("activites"));
    var journalApres = (List<Map<String, Object>>) ((Map<String, Object>) apres.get("suivi")).get("journal");
    assertThat(CucumberRestTestContext.getElement("$.dossier.suivi.journal")).isEqualTo(journalApres);
    var idsAvant = ((List<Map<String, Object>>) avant.get("journal")).stream()
      .map(fait -> fait.get("id"))
      .toList();
    var nouveaux = journalApres
      .stream()
      .map(fait -> fait.get("id"))
      .filter(id -> !idsAvant.contains(id))
      .toList();
    if (nouveaux.isEmpty()) {
      assertThat(CucumberRestTestContext.getElement("$.recu.evenementCree")).isNull();
    } else {
      assertThat(nouveaux).containsExactly(CucumberRestTestContext.getElement("$.recu.evenementCree"));
    }
  }

  @Then("le recu canonique conserve les memes identites et activites")
  public void verifie() {
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("ENREGISTREE");
    assertThat(CucumberRestTestContext.getElement("$.recu")).isEqualTo(recu);
    assertThat(CucumberRestTestContext.getElement("$.dossier.activites")).isEqualTo(apres.get("activites"));
  }

  @Then("la liste des conflits de {string} reste vide")
  @SuppressWarnings("unchecked")
  public void temoinHorsListe(String alias) {
    atelier.jeConsulte(alias);
    var courant = (Map<String, Object>) CucumberRestTestContext.getElement("$");
    element = (String) courant.get("element");
    assertThat((List<?>) courant.get("conflits")).isEmpty();
    liste(0);
  }

  @Then("la liste de ce suivi conserve {int} sequences en conflit")
  public void liste(int nombre) {
    rest.get("/api/atelier/conflits?element=" + element);
    assertThatLastResponse().hasOkStatus().hasElement("$.complete").withValue(true).and().hasElement("$.total").withValue(nombre);
  }

  @Then("les periodes relues de {string} ont les durees")
  @SuppressWarnings("unchecked")
  public void durees(String alias, List<String> attendues) {
    atelier.jeConsulteLeTempsEffectifDe(alias);
    var intervalles = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$");
    assertThat(
      intervalles
        .stream()
        .map(intervalle ->
          Duration.between(Instant.parse((String) intervalle.get("debut")), Instant.parse((String) intervalle.get("fin"))).toString()
        )
        .toList()
    ).containsExactlyElementsOf(attendues);
  }
}

package com.glm.glmback.atelier.cucumber.gestionconflits;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.atelier.cucumber.AtelierSteps;
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
  private String ancre;
  private String element;
  private String commande;
  private Map<String, Object> proposition;
  private long revision;
  private Map<String, Object> avant;
  private Object tempsAvant;
  private Map<String, Object> apres;
  private Map<String, Object> acte;
  private Object recu;
  private Map<String, Object> projectionsAvant;
  private Map<String, Object> projectionsApres;

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
    projectionsAvant = litProjections();
    rest.get("/api/atelier/suivis/" + suivi + "/conflits/" + journal.get(ancre).get("id"));
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("EN_CONFLIT");
    revision = ((Number) CucumberRestTestContext.getElement("$.revision")).longValue();
    this.ancre = (String) journal.get(ancre).get("id");
    commande = UUID.randomUUID().toString();
    rest.post(
      "/api/atelier/suivis/" + suivi + "/conflits/" + journal.get(ancre).get("id") + "/apercus",
      JSON.writeValueAsString(Map.of("commande", commande, "revision", revision, "acte", acte))
    );
    assertThatLastResponse().hasOkStatus();
    proposition = new java.util.HashMap<>();
    for (var champ : java.util.List.of("commande", "adresse", "revision", "acte", "empreinteConsequences", "evenement")) {
      var valeur = CucumberRestTestContext.getElement("$." + champ);
      if (valeur != null) {
        proposition.put(champ, valeur);
      }
    }
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
    rest.get("/api/atelier/suivis/" + suivi + "/conflits/" + ancre);
    assertThat(((Number) CucumberRestTestContext.getElement("$.revision")).longValue()).isEqualTo(revision);
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("NON_ATTESTEE");
    assertThat(litProjections()).isEqualTo(projectionsAvant);
  }

  @When("je confirme cet apercu de resolution")
  @SuppressWarnings("unchecked")
  public void confirme() {
    rest.post("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution", JSON.writeValueAsString(proposition));
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
    projectionsApres = litProjections();
  }

  @Then("le recu canonique conserve les memes identites et activites")
  public void verifie() {
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("ENREGISTREE");
    assertThat(CucumberRestTestContext.getElement("$.recu")).isEqualTo(recu);
    assertThat(CucumberRestTestContext.getElement("$.dossier.activites")).isEqualTo(apres.get("activites"));
  }

  @Then("l'ancre corrigee est explicitement annulee")
  public void ancreAnnulee() {
    rest.get("/api/atelier/suivis/" + suivi + "/conflits/" + acte.get("pointage"));
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("ANCRE_ANNULEE");
    var journalApres = ((Map<?, ?>) apres.get("suivi")).get("journal");
    rest.post(
      "/api/atelier/suivis/" + suivi + "/conflits/" + acte.get("pointage") + "/apercus",
      JSON.writeValueAsString(Map.of("commande", UUID.randomUUID(), "revision", revision + 1, "acte", acte))
    );
    assertThatLastResponse().hasHttpStatus(409);
    rest.get("/api/atelier/suivis/" + suivi);
    assertThat(CucumberRestTestContext.getElement("$.journal")).isEqualTo(journalApres);
    assertThat(litProjections()).isEqualTo(projectionsApres);
    rest.get("/api/atelier/suivis/" + suivi + "/conflits/" + acte.get("pointage"));
    assertThat(((Number) CucumberRestTestContext.getElement("$.revision")).longValue()).isEqualTo(revision + 1);
  }

  @Then("les faits independants de cet acte restent identiques")
  @SuppressWarnings("unchecked")
  public void faitsIndependants(List<String> rangs) {
    var originaux = (List<Map<String, Object>>) avant.get("journal");
    var actuels = (List<Map<String, Object>>) ((Map<String, Object>) apres.get("suivi")).get("journal");
    for (String rang : rangs) {
      var original = originaux.get(Integer.parseInt(rang));
      assertThat(actuels)
        .filteredOn(fait -> original.get("id").equals(fait.get("id")))
        .containsExactly(original);
    }
  }

  @Then("les lignes restantes portent la revision commune")
  @SuppressWarnings("unchecked")
  public void revisionsCommunes() {
    rest.get("/api/atelier/conflits?element=" + element);
    assertThatLastResponse().hasOkStatus();
    var lignes = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.lignes");
    assertThat(lignes)
      .isNotEmpty()
      .allSatisfy(ligne -> {
        assertThat(((Number) ligne.get("revision")).longValue()).isEqualTo(revision + 1);
      });
  }

  @Then("les API lecteurs conservent la duree nanoseconde dans la semaine {int}")
  @SuppressWarnings("unchecked")
  public void lecteursNanoseconde(int semaine) {
    var journal = (List<Map<String, Object>>) avant.get("journal");
    var operateur = (String) journal.getFirst().get("operateurId");
    var attendue = ((List<Map<String, Object>>) apres.get("activites")).getFirst();
    rest.get("/api/feuilles-de-temps/" + operateur + "?annee=2044&semaine=" + semaine);
    assertThatLastResponse().hasOkStatus();
    var portions = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours")).stream()
      .flatMap(jour -> ((List<Map<String, Object>>) jour.get("activites")).stream())
      .toList();
    assertThat(portions)
      .singleElement()
      .satisfies(portion -> {
        assertThat(portion.get("debut")).isEqualTo(attendue.get("debut"));
        assertThat(portion.get("fin")).isEqualTo(attendue.get("fin"));
        assertThat(((Map<String, Object>) portion.get("activite")).get("id")).isEqualTo(attendue.get("activite"));
      });
    rest.get("/api/syntheses-des-heures/" + operateur + "?annee=2044&semaine=" + semaine);
    assertThatLastResponse().hasOkStatus().hasElement("$.dureeOperationnelleTotale.valeur").withValue("PT0.000000001S");
    rest.get("/api/couts-de-revient/" + element);
    assertThatLastResponse().hasOkStatus().hasElement("$.temps.total.valeur").withValue("PT0.000000001S");
    assertThat(CucumberRestTestContext.getElement("$.cout.total.complete")).isEqualTo(true);
    assertThat(new java.math.BigDecimal(CucumberRestTestContext.getElement("$.cout.total.valeur").toString())).isZero();
    rest.get("/api/pupitre/referentiel");
    assertThatLastResponse().hasOkStatus();
    var tuile = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.suivis")).stream()
      .filter(ligne -> suivi.equals(ligne.get("id")))
      .findFirst()
      .orElseThrow();
    assertThat((List<?>) tuile.get("activites")).isEmpty();
    assertThat((List<?>) tuile.get("conflits")).isEmpty();
  }

  @Then("les API lecteurs gardent le travail en cours sans duree finale")
  @SuppressWarnings("unchecked")
  public void lecteursEnCours() {
    var journal = (List<Map<String, Object>>) avant.get("journal");
    var operateur = (String) journal.getFirst().get("operateurId");
    rest.get("/api/pupitre/referentiel");
    assertThatLastResponse().hasOkStatus();
    var tuile = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.suivis")).stream()
      .filter(ligne -> suivi.equals(ligne.get("id")))
      .findFirst()
      .orElseThrow();
    assertThat((List<Map<String, Object>>) tuile.get("activites"))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.get("ouverture")).isEqualTo(journal.getFirst().get("activite"));
        assertThat(activite.get("echeance")).isEqualTo("2044-01-13T21:00:00Z");
      });
    rest.get("/api/feuilles-de-temps/" + operateur + "?annee=2044&semaine=2");
    assertThatLastResponse().hasOkStatus();
    var portions = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours")).stream()
      .flatMap(jour -> ((List<Map<String, Object>>) jour.get("activites")).stream())
      .toList();
    assertThat(portions)
      .singleElement()
      .satisfies(portion -> {
        assertThat(portion.get("fin")).isNull();
        assertThat(((Map<String, Object>) portion.get("activite")).get("etat")).isEqualTo("EN_COURS");
      });
    rest.get("/api/syntheses-des-heures/" + operateur + "?annee=2044&semaine=2");
    assertThatLastResponse().hasOkStatus().hasElement("$.dureeOperationnelleTotale.valeur").withValue("PT0S");
    rest.get("/api/couts-de-revient/" + element);
    assertThatLastResponse()
      .hasOkStatus()
      .hasElement("$.activitesEnCours")
      .withValue(1)
      .and()
      .hasElement("$.temps.total.valeur")
      .withValue("PT0S");
  }

  @Then("les API lecteurs jugent le travail echu a vingt et une heures")
  @SuppressWarnings("unchecked")
  public void lecteursEchus() {
    var journal = (List<Map<String, Object>>) avant.get("journal");
    var operateur = (String) journal.getFirst().get("operateurId");
    rest.get("/api/pupitre/referentiel");
    assertThatLastResponse().hasOkStatus();
    var tuile = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.suivis")).stream()
      .filter(ligne -> suivi.equals(ligne.get("id")))
      .findFirst()
      .orElseThrow();
    assertThat((List<?>) tuile.get("activites")).isEmpty();
    assertThat((List<?>) tuile.get("conflits")).isEmpty();
    rest.get("/api/feuilles-de-temps/" + operateur + "?annee=2044&semaine=2");
    assertThatLastResponse().hasOkStatus();
    var portions = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours")).stream()
      .flatMap(jour -> ((List<Map<String, Object>>) jour.get("activites")).stream())
      .toList();
    assertThat(portions)
      .singleElement()
      .satisfies(portion -> {
        assertThat(portion.get("fin")).isEqualTo("2044-01-13T21:00:00Z");
        assertThat(((Map<String, Object>) portion.get("activite")).get("etat")).isEqualTo("TERMINEE_AUTOMATIQUEMENT");
      });
    rest.get("/api/syntheses-des-heures/" + operateur + "?annee=2044&semaine=2");
    assertThatLastResponse().hasOkStatus().hasElement("$.dureeOperationnelleTotale.valeur").withValue("PT13H");
    rest.get("/api/couts-de-revient/" + element);
    assertThatLastResponse()
      .hasOkStatus()
      .hasElement("$.activitesEnCours")
      .withValue(0)
      .and()
      .hasElement("$.temps.total.valeur")
      .withValue("PT13H");
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasOkStatus().hasElement("$.dossier.activites[0].etat").withValue("ECHUE");
    assertThat(CucumberRestTestContext.getElement("$.recu")).isEqualTo(recu);
    assertThat(CucumberRestTestContext.getElement("$.dossier.activites[0].duree")).isEqualTo("PT13H");
  }

  @When("je tente la confirmation de cet apercu avec statut {int}")
  public void confirmationRefusee(int statut) {
    rest.post("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution", JSON.writeValueAsString(proposition));
    assertThatLastResponse().hasHttpStatus(statut);
  }

  @When("je tente la verification de cette commande avec statut {int}")
  public void verificationRefusee(int statut) {
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasHttpStatus(statut);
  }

  @When("je tente un nouvel apercu avec statut {int}")
  public void apercuRefuse(int statut) {
    rest.post(
      "/api/atelier/suivis/" + suivi + "/conflits/" + ancre + "/apercus",
      JSON.writeValueAsString(Map.of("commande", UUID.randomUUID(), "revision", revision, "acte", acte))
    );
    assertThatLastResponse().hasHttpStatus(statut);
  }

  @Then("la commande reste non attestee pour cet autre tenant")
  public void nonAttesteeAutreTenant() {
    rest.get("/api/atelier/suivis/" + suivi + "/confirmations-de-resolution/" + commande);
    assertThatLastResponse().hasOkStatus().hasElement("$.kind").withValue("NON_ATTESTEE");
  }

  @Then("le suivi garde exactement les faits confirmes et leur revision")
  public void faitsConfirmesInchanges() {
    rest.get("/api/atelier/suivis/" + suivi);
    assertThat(CucumberRestTestContext.getElement("$")).isEqualTo(apres.get("suivi"));
    rest.get("/api/atelier/suivis/" + suivi + "/conflits/" + ancre);
    assertThat(((Number) CucumberRestTestContext.getElement("$.revision")).longValue()).isEqualTo(revision + 1);
    assertThat(litProjections()).isEqualTo(projectionsApres);
  }

  @Then("les apercus invalides restent refuses sans ecriture")
  @SuppressWarnings("unchecked")
  public void apercusInvalides() {
    for (String variation : List.of(
      "motif vide",
      "motif blanc",
      "futur nanoseconde",
      "instant invalide",
      "intention incoherente",
      "cible absente"
    )) {
      var invalide = new HashMap<>(acte);
      var fait = new HashMap<>((Map<String, Object>) acte.get("fait"));
      switch (variation) {
        case "motif vide" -> invalide.put("motif", "");
        case "motif blanc" -> invalide.put("motif", "   ");
        case "futur nanoseconde" -> fait.put("instant", "2044-01-31T18:00:00.000000001Z");
        case "instant invalide" -> fait.put("instant", "2044-01-31");
        case "intention incoherente" -> fait.put("type", "DEBUT");
        case "cible absente" -> fait.remove("activiteVisee");
        default -> throw new IllegalArgumentException(variation);
      }
      invalide.put("fait", fait);
      rest.post(
        "/api/atelier/suivis/" + suivi + "/conflits/" + ancre + "/apercus",
        JSON.writeValueAsString(Map.of("commande", UUID.randomUUID(), "revision", revision, "acte", invalide))
      );
      assertThatLastResponse().hasHttpStatus(400);
      sansEcriture();
    }
  }

  @Then("les quatre lecteurs API expliquent ce conflit")
  @SuppressWarnings("unchecked")
  public void lecteursAvant() {
    var journal = (List<Map<String, Object>>) avant.get("journal");
    var operateur = (String) journal.getFirst().get("operateurId");
    rest.get("/api/pupitre/referentiel");
    assertThatLastResponse().hasOkStatus();
    var tuile = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.suivis")).stream()
      .filter(ligne -> suivi.equals(ligne.get("id")))
      .findFirst()
      .orElseThrow();
    assertThat((List<?>) tuile.get("conflits")).hasSize(1);
    rest.get("/api/feuilles-de-temps/" + operateur + "?annee=2044&semaine=1");
    assertThatLastResponse().hasOkStatus();
    var portions = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours")).stream()
      .flatMap(jour -> ((List<Map<String, Object>>) jour.get("activites")).stream())
      .toList();
    assertThat(portions)
      .hasSize(2)
      .allSatisfy(portion -> {
        var activite = (Map<String, Object>) portion.get("activite");
        assertThat(activite.get("etat")).isEqualTo("A_RESOUDRE");
        assertThat(activite.get("fin")).isNull();
      });
    rest.get("/api/syntheses-des-heures/" + operateur + "?annee=2044&semaine=1");
    assertThatLastResponse().hasOkStatus().hasElement("$.conflits").withElementsCount(1);
    assertThat(CucumberRestTestContext.getElement("$.dureeOperationnelleTotale.complete")).isEqualTo(false);
    assertThat(CucumberRestTestContext.getElement("$.dureeOperationnelleTotale.valeur")).isNull();
    rest.get("/api/couts-de-revient/" + element);
    assertThatLastResponse().hasOkStatus().hasElement("$.conflits").withElementsCount(1);
    assertThat(CucumberRestTestContext.getElement("$.temps.total.complete")).isEqualTo(false);
    assertThat(CucumberRestTestContext.getElement("$.temps.total.valeur")).isNull();
    assertThat(CucumberRestTestContext.getElement("$.cout.total.complete")).isEqualTo(false);
    assertThat(CucumberRestTestContext.getElement("$.cout.total.valeur")).isNull();
  }

  @Then("les quatre lecteurs API relevent la correction exacte")
  @SuppressWarnings("unchecked")
  public void lecteursApres() {
    var journal = (List<Map<String, Object>>) avant.get("journal");
    var operateur = (String) journal.getFirst().get("operateurId");
    rest.get("/api/pupitre/referentiel");
    assertThatLastResponse().hasOkStatus();
    var tuile = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.suivis")).stream()
      .filter(ligne -> suivi.equals(ligne.get("id")))
      .findFirst()
      .orElseThrow();
    assertThat((List<?>) tuile.get("conflits")).isEmpty();
    assertThat((List<?>) tuile.get("activites")).isEmpty();
    rest.get("/api/feuilles-de-temps/" + operateur + "?annee=2044&semaine=1");
    assertThatLastResponse().hasOkStatus();
    var portions = ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.jours")).stream()
      .flatMap(jour -> ((List<Map<String, Object>>) jour.get("activites")).stream())
      .toList();
    var activites = (List<Map<String, Object>>) apres.get("activites");
    assertThat(portions).hasSize(activites.size());
    for (int index = 0; index < portions.size(); index++) {
      var portion = portions.get(index);
      var interpretation = (Map<String, Object>) portion.get("activite");
      var attendue = activites.get(index);
      assertThat(portion.get("element")).isEqualTo(element);
      assertThat(portion.get("categorie")).isEqualTo(attendue.get("categorie"));
      assertThat(portion.get("debut")).isEqualTo(attendue.get("debut"));
      assertThat(portion.get("fin")).isEqualTo(attendue.get("fin"));
      assertThat(interpretation.get("id")).isEqualTo(attendue.get("activite"));
      assertThat(interpretation.get("etat")).isEqualTo("TERMINEE");
    }
    rest.get("/api/syntheses-des-heures/" + operateur + "?annee=2044&semaine=1");
    assertThatLastResponse().hasOkStatus().hasElement("$.conflits").withElementsCount(0);
    assertThat(CucumberRestTestContext.getElement("$.dureeOperationnelleTotale.complete")).isEqualTo(true);
    assertThat(CucumberRestTestContext.getElement("$.dureeOperationnelleTotale.valeur")).isEqualTo("PT9H");
    assertThat(CucumberRestTestContext.getElement("$.elements[0].dureeNonConformite.valeur")).isEqualTo("PT5H");
    rest.get("/api/couts-de-revient/" + element);
    assertThatLastResponse().hasOkStatus().hasElement("$.conflits").withElementsCount(0);
    assertThat(CucumberRestTestContext.getElement("$.temps.total.complete")).isEqualTo(true);
    assertThat(CucumberRestTestContext.getElement("$.temps.total.valeur")).isEqualTo("PT9H");
    assertThat(CucumberRestTestContext.getElement("$.cout.total.complete")).isEqualTo(true);
    assertThat(new java.math.BigDecimal(CucumberRestTestContext.getElement("$.cout.machine.valeur").toString())).isEqualByComparingTo(
      "409.50"
    );
    assertThat(new java.math.BigDecimal(CucumberRestTestContext.getElement("$.cout.mainDOeuvre.valeur").toString())).isEqualByComparingTo(
      "198.00"
    );
    assertThat(new java.math.BigDecimal(CucumberRestTestContext.getElement("$.cout.total.valeur").toString())).isEqualByComparingTo(
      "607.50"
    );
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

  private Map<String, Object> litProjections() {
    rest.get("/api/atelier/conflits?element=" + element);
    assertThatLastResponse().hasOkStatus();
    Object liste = CucumberRestTestContext.getElement("$");
    rest.get("/api/couts-de-revient/" + element);
    assertThatLastResponse().hasOkStatus();
    return Map.of("liste", liste, "cout", CucumberRestTestContext.getElement("$"));
  }
}

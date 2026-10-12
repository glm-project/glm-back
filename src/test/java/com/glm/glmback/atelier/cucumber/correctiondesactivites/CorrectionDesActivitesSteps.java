package com.glm.glmback.atelier.cucumber.correctiondesactivites;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.atelier.cucumber.AtelierSteps;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * La correction de la journee d'un operateur, vue du client HTTP : trois ecritures sur le suivi, une lecture dediee.
 *
 * <p>
 * Une activite se designe par son operateur et l'heure de son debut, jamais par un identifiant : l'identifiant d'une
 * activite est celui de son pointage ouvrant, et le serveur le donne dans le journal du suivi.
 * </p>
 */
public class CorrectionDesActivitesSteps {

  private static final String SUIVIS_URI = "/api/atelier/suivis/";
  private static final String OPERATEURS_URI = "/api/atelier/operateurs/";
  private static final String SYNTHESES_URI = "/api/syntheses-des-heures/";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private AtelierSteps atelier;

  private String derniereCorrectionUri;
  private String derniereCorrectionCorps;

  @When("j'ajoute sur {string} une activite")
  public void jAjouteUneActivite(String alias, Map<String, String> donnees) {
    Map<String, String> corps = new HashMap<>(donnees);
    corps.computeIfPresent("operateur", (cle, valeur) -> atelier.operateurId(valeur));
    corps.computeIfPresent("poste", (cle, valeur) -> atelier.posteId(valeur));
    corps.putIfAbsent("id", UUID.randomUUID().toString());
    envoie(SUIVIS_URI + atelier.suiviId(alias) + "/activites", corps);
  }

  @Given("j'ai ajoute sur {string} une activite")
  public void jaiAjouteUneActivite(String alias, Map<String, String> donnees) {
    jAjouteUneActivite(alias, donnees);
    assertThatLastResponse().hasHttpStatus(201);
  }

  @When("je change sur {string} le poste de l'activite de {string} commencee a {string}")
  public void jeChangeLePoste(String alias, String operateur, String debut, Map<String, String> donnees) {
    Map<String, String> corps = new HashMap<>(donnees);
    corps.computeIfPresent("poste", (cle, valeur) -> atelier.posteId(valeur));
    corps.putIfAbsent("id", UUID.randomUUID().toString());
    envoie(SUIVIS_URI + atelier.suiviId(alias) + "/activites/" + activite(alias, operateur, debut) + "/poste", corps);
  }

  @Given("j'ai change sur {string} le poste de l'activite de {string} commencee a {string}")
  public void jaiChangeLePoste(String alias, String operateur, String debut, Map<String, String> donnees) {
    jeChangeLePoste(alias, operateur, debut, donnees);
    assertThatLastResponse().hasHttpStatus(201);
  }

  @When("je change le poste de l'activite inconnue sur {string}")
  public void jeChangeLePosteDeLActiviteInconnue(String alias, Map<String, String> donnees) {
    Map<String, String> corps = new HashMap<>(donnees);
    corps.computeIfPresent("poste", (cle, valeur) -> atelier.posteId(valeur));
    corps.putIfAbsent("id", UUID.randomUUID().toString());
    envoie(SUIVIS_URI + atelier.suiviId(alias) + "/activites/" + UUID.randomUUID() + "/poste", corps);
  }

  @When("je retire sur {string} l'activite de {string} commencee a {string}")
  public void jeRetire(String alias, String operateur, String debut) {
    envoie(
      SUIVIS_URI + atelier.suiviId(alias) + "/activites/" + activite(alias, operateur, debut) + "/retrait",
      Map.of("id", UUID.randomUUID().toString())
    );
  }

  @Given("j'ai retire sur {string} l'activite de {string} commencee a {string}")
  public void jaiRetire(String alias, String operateur, String debut) {
    jeRetire(alias, operateur, debut);
    assertThatLastResponse().hasHttpStatus(201);
  }

  @When("je retire l'activite inconnue sur {string}")
  public void jeRetireLActiviteInconnue(String alias) {
    envoie(
      SUIVIS_URI + atelier.suiviId(alias) + "/activites/" + UUID.randomUUID() + "/retrait",
      Map.of("id", UUID.randomUUID().toString())
    );
  }

  @When("je retire une activite sur le suivi inconnu")
  public void jeRetireSurLeSuiviInconnu() {
    envoie(SUIVIS_URI + UUID.randomUUID() + "/activites/" + UUID.randomUUID() + "/retrait", Map.of("id", UUID.randomUUID().toString()));
  }

  @When("je renvoie la derniere correction")
  public void jeRenvoieLaDerniereCorrection() {
    rest.post(derniereCorrectionUri, derniereCorrectionCorps);
  }

  @When("j'envoie une correction sans corps sur {string}")
  public void jEnvoieUneCorrectionSansCorps(String alias) {
    envoie(SUIVIS_URI + atelier.suiviId(alias) + "/activites/" + UUID.randomUUID() + "/retrait", Map.of());
  }

  @When("je pointe un {string} sur {string}")
  public void jePointeUneCorrection(String type, String alias) {
    atelier.jePointeSur(alias, Map.of("type", type, "operateur", UUID.randomUUID().toString()));
  }

  @When("je consulte la journee de {string} du {string}")
  public void jeConsulteLaJournee(String operateur, String jour) {
    rest.get(OPERATEURS_URI + atelier.operateurId(operateur) + "/journees/" + jour);
  }

  @When("je consulte la journee de {string} du {string} a l'evaluation {string}")
  public void jeConsulteLaJourneeALEvaluation(String operateur, String jour, String evaluation) {
    rest.get(
      OPERATEURS_URI + atelier.operateurId(operateur) + "/journees/" + jour + "?evaluation={evaluation}",
      Map.of("evaluation", evaluation)
    );
  }

  @When("je consulte la journee de l'operateur inconnu du {string}")
  public void jeConsulteLaJourneeDeLOperateurInconnu(String jour) {
    rest.get(OPERATEURS_URI + UUID.randomUUID() + "/journees/" + jour);
  }

  @When("je relis la synthese des heures de {string} pour la semaine {int} de {int}")
  public void jeRelisLaSyntheseDesHeures(String operateur, int semaine, int annee) {
    rest.get(SYNTHESES_URI + atelier.operateurId(operateur) + "?annee=" + annee + "&semaine=" + semaine);
  }

  @Then("la journee porte les bornes")
  public void laJourneePorteLesBornes(Map<String, String> attendues) {
    attendues.forEach((champ, valeur) -> assertThat(CucumberRestTestContext.getElement("$.bornes." + champ)).as(champ).isEqualTo(valeur));
  }

  @Then("la journee est celle de {string} le {string} a l'evaluation {string}")
  public void laJourneeEstCelleDe(String operateur, String jour, String evaluation) {
    assertThat(CucumberRestTestContext.getElement("$.operateur.id")).isEqualTo(atelier.operateurId(operateur));
    assertThat(CucumberRestTestContext.getElement("$.operateur.nom")).isEqualTo(operateur);
    assertThat(CucumberRestTestContext.getElement("$.jour")).isEqualTo(jour);
    assertThat(CucumberRestTestContext.getElement("$.evaluation")).isEqualTo(evaluation);
  }

  @Then("la duree operationnelle de la journee est {string}")
  public void laDureeOperationnelleDeLaJourneeEst(String duree) {
    assertThat(CucumberRestTestContext.getElement("$.dureeOperationnelle")).isEqualTo(duree);
  }

  /**
   * Les activites rendues, dans l'ordre, reduites aux colonnes du tableau. Une colonne vide attend une valeur absente ;
   * {@code poste} se donne par l'alias du poste, {@code autresJours} par {@code jour=duree}, separes par des virgules.
   */
  @Then("la journee porte les activites")
  public void laJourneePorteLesActivites(List<Map<String, String>> attendues) {
    List<Map<String, Object>> lues = activitesLues();

    assertThat(lues).hasSize(attendues.size());
    for (int rang = 0; rang < attendues.size(); rang++) {
      Map<String, Object> lue = lues.get(rang);
      int numero = rang;
      attendues
        .get(rang)
        .forEach((colonne, valeur) ->
          assertThat(valeurLue(lue, colonne)).as("activite %d, %s", numero, colonne).isEqualTo(valeurAttendue(valeur))
        );
    }
  }

  @Then("la journee ne porte aucune activite")
  public void laJourneeNePorteAucuneActivite() {
    assertThat(activitesLues()).isEmpty();
  }

  @Then("l'activite de la journee commencee a {string} porte l'element {string} de reference {string}")
  public void lActiviteDeLaJourneePorteLElement(String debut, String nom, String reference) {
    Map<String, Object> activite = activiteLue(debut);
    @SuppressWarnings("unchecked")
    Map<String, Object> element = (Map<String, Object>) activite.get("element");

    assertThat(element.get("categorie")).isEqualTo("OF");
    assertThat(element.get("reference")).isEqualTo(reference);
    assertThat(String.valueOf(element.get("nom"))).isNotBlank();
  }

  @Then("l'activite de la journee commencee a {string} est sur le suivi de {string}")
  public void lActiviteDeLaJourneeEstSurLeSuivi(String debut, String alias) {
    assertThat(activiteLue(debut).get("suivi")).isEqualTo(atelier.suiviId(alias));
  }

  @Then("la journee propose les postes")
  public void laJourneeProposeLesPostes(List<Map<String, String>> attendus) {
    List<Map<String, Object>> postes = lus("$.choix.postes");

    assertThat(postes).hasSize(attendus.size());
    for (int rang = 0; rang < attendus.size(); rang++) {
      Map<String, Object> poste = postes.get(rang);
      assertThat(poste.get("id")).isEqualTo(atelier.posteId(attendus.get(rang).get("poste")));
      assertThat(poste.get("libelle")).isEqualTo(attendus.get(rang).get("poste"));
      assertThat(poste.get("nature")).isEqualTo(attendus.get(rang).get("nature"));
    }
  }

  @Then("la journee propose le suivi de {string} engage le {string}")
  public void laJourneeProposeLeSuivi(String alias, String engageLe) {
    assertThat(suiviPropose(alias)).containsEntry("engageLe", engageLe);
  }

  @Then("la journee propose le suivi de {string} engage le {string} et cloture le {string}")
  public void laJourneeProposeLeSuiviCloture(String alias, String engageLe, String clotureLe) {
    assertThat(suiviPropose(alias)).containsEntry("engageLe", engageLe).containsEntry("clotureLe", clotureLe);
  }

  @Then("la journee ne propose pas le suivi de {string}")
  public void laJourneeNeProposePasLeSuivi(String alias) {
    assertThat(lus("$.choix.suivis")).noneMatch(suivi -> Objects.equals(suivi.get("suivi"), atelier.suiviId(alias)));
  }

  @Then("le journal du suivi se termine par les types")
  public void leJournalDuSuiviSeTerminePar(List<String> types) {
    @SuppressWarnings("unchecked")
    List<String> journal = (List<String>) CucumberRestTestContext.getElement("$.journal[*].type");

    assertThat(journal.subList(journal.size() - types.size(), journal.size())).containsExactlyElementsOf(types);
  }

  @Then("le dernier evenement du journal du suivi est un fait du gestionnaire de type {string} date de {string}")
  public void leDernierEvenementEstUnFaitDuGestionnaire(String type, String date) {
    int dernier = ((List<?>) CucumberRestTestContext.getElement("$.journal")).size() - 1;
    String chemin = "$.journal[" + dernier + "].";

    assertThat(CucumberRestTestContext.getElement(chemin + "type")).isEqualTo(type);
    assertThat(CucumberRestTestContext.getElement(chemin + "estUneRegularisation")).isEqualTo(true);
    assertThat(CucumberRestTestContext.getElement(chemin + "dateDeSurvenue")).isEqualTo(date);
    assertThat(CucumberRestTestContext.getElement(chemin + "dateDEnregistrement")).isEqualTo(date);
    assertThat(CucumberRestTestContext.getElement(chemin + "auteur")).isEqualTo("gestionnaire");
    assertThat(CucumberRestTestContext.getElement(chemin + "activite")).isNull();
    assertThat(CucumberRestTestContext.getElement(chemin + "activiteVisee")).isNotNull();
  }

  /**
   * Le serveur ecrit dans la fin pointee l'activite qu'il jugeait en cours : c'est son lien, aucune lecture ne le devine.
   * Le journal est lu par le steps precedent, l'ordre du journal est celui des heures : la fin pointee est la derniere
   * fin non regularisee.
   */
  @Then("le dernier evenement du journal du suivi est une fin pointee qui vise l'activite de {string} commencee a {string}")
  public void leDernierEvenementEstUneFinPointeeQuiVise(String operateur, String debut) {
    String attendue = activiteLueDansLeJournal(operateur, debut);
    List<Map<String, Object>> fins = lus("$.journal")
      .stream()
      .filter(fait -> "FIN".equals(fait.get("type")) && Boolean.FALSE.equals(fait.get("estUneRegularisation")))
      .toList();

    assertThat(fins).isNotEmpty();
    assertThat(fins.getLast().get("activiteVisee")).isEqualTo(attendue);
    assertThat(fins.getLast().get("activite")).isNull();
  }

  @Then("le fait {string} du journal du suivi a la nature {string} et le cout horaire {string}")
  public void leFaitDuJournalALaNature(String type, String nature, String coutHoraire) {
    @SuppressWarnings("unchecked")
    List<String> types = (List<String>) CucumberRestTestContext.getElement("$.journal[*].type");
    int rang = types.lastIndexOf(type);

    assertThat(rang).as("un fait %s au journal", type).isGreaterThanOrEqualTo(0);
    assertThat(CucumberRestTestContext.getElement("$.journal[" + rang + "].nature")).isEqualTo(nature);
    assertThat(String.valueOf(CucumberRestTestContext.getElement("$.journal[" + rang + "].coutHoraire"))).isEqualTo(coutHoraire);
  }

  private void envoie(String uri, Map<String, ?> corps) {
    derniereCorrectionUri = uri;
    derniereCorrectionCorps = JSON.writeValueAsString(corps);
    rest.post(derniereCorrectionUri, derniereCorrectionCorps);
  }

  /**
   * L'identifiant de l'activite que l'operateur a ouverte a cette heure sur ce suivi, lu dans son journal.
   */
  private String activite(String alias, String operateur, String debut) {
    rest.get(SUIVIS_URI + atelier.suiviId(alias));

    return activiteLueDansLeJournal(operateur, debut);
  }

  private String activiteLueDansLeJournal(String operateur, String debut) {
    String operateurId = atelier.operateurId(operateur);
    Instant instant = Instant.parse(debut);

    return lus("$.journal")
      .stream()
      .filter(fait -> fait.get("activite") != null)
      .filter(fait -> operateurId.equals(fait.get("operateurId")))
      .filter(fait -> instant.equals(Instant.parse((String) fait.get("dateDeSurvenue"))))
      .map(fait -> (String) fait.get("activite"))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Aucune activite de " + operateur + " commencee a " + debut));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> lus(String chemin) {
    return (List<Map<String, Object>>) CucumberRestTestContext.getElement(chemin);
  }

  private static List<Map<String, Object>> activitesLues() {
    return lus("$.activites");
  }

  private static Map<String, Object> activiteLue(String debut) {
    return activitesLues()
      .stream()
      .filter(activite -> Instant.parse(debut).equals(Instant.parse((String) activite.get("debut"))))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Aucune activite lue ne commence a " + debut));
  }

  private Map<String, Object> suiviPropose(String alias) {
    return lus("$.choix.suivis")
      .stream()
      .filter(suivi -> Objects.equals(suivi.get("suivi"), atelier.suiviId(alias)))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Le suivi de " + alias + " n'est pas propose"));
  }

  @SuppressWarnings("unchecked")
  private Object valeurLue(Map<String, Object> activite, String colonne) {
    return switch (colonne) {
      case "poste" -> Objects.requireNonNullElse(
        activite.get("poste") == null ? null : ((Map<String, Object>) activite.get("poste")).get("libelle"),
        ""
      );
      case "nature" -> Objects.requireNonNullElse(
        activite.get("poste") == null ? null : ((Map<String, Object>) activite.get("poste")).get("nature"),
        ""
      );
      case "autresJours" -> String.join(
        ",",
        ((List<Map<String, Object>>) activite.get("autresJours")).stream()
          .map(portion -> portion.get("jour") + "=" + portion.get("duree"))
          .toList()
      );
      default -> Objects.requireNonNullElse(activite.get(colonne), "");
    };
  }

  private static String valeurAttendue(String valeur) {
    return valeur == null ? "" : valeur;
  }
}

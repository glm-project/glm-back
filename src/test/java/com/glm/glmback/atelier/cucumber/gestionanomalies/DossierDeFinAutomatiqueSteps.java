package com.glm.glmback.atelier.cucumber.gestionanomalies;

import static com.glm.glmback.cucumber.rest.CucumberRestAssertions.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.atelier.cucumber.AtelierSteps;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * La lecture du dossier d'anomalie ouvert sur une fin automatique.
 *
 * <p>
 * Les evenements se designent par leur rang dans le journal du suivi : les identifiants d'evenement et d'activite
 * viennent du serveur, jamais d'une valeur supposee.
 * </p>
 */
public class DossierDeFinAutomatiqueSteps {

  @Autowired
  private CucumberRestClient rest;

  @Autowired
  private AtelierSteps atelier;

  private String suivi;
  private List<Map<String, Object>> journal;
  private Map<String, Object> dossier;

  @When("je consulte le dossier d'anomalie de {string} depuis l'evenement {int}")
  @SuppressWarnings("unchecked")
  public void consulte(String alias, int rang) {
    atelier.jeConsulte(alias);
    var suiviLu = (Map<String, Object>) CucumberRestTestContext.getElement("$");
    suivi = (String) suiviLu.get("id");
    journal = (List<Map<String, Object>>) suiviLu.get("journal");
    rest.get("/api/atelier/suivis/" + suivi + "/anomalies/" + journal.get(rang).get("id"));
    assertThatLastResponse().hasOkStatus();
    dossier = map(CucumberRestTestContext.getElement("$"));
  }

  @Then("le dossier d'anomalie est a l'etat {string}")
  public void etat(String etat) {
    assertThat(dossier.get("kind")).isEqualTo(etat);
  }

  @Then("le dossier d'anomalie signale une fin automatique")
  public void signaleUneFinAutomatique() {
    assertThat(dossier.get("finAutomatique")).isEqualTo(true);
  }

  @Then("le dossier d'anomalie declare finAutomatique {string}")
  public void declareFinAutomatique(String valeur) {
    assertThat(dossier.get("finAutomatique")).isEqualTo(Boolean.valueOf(valeur));
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

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object valeur) {
    return (Map<String, Object>) valeur;
  }
}

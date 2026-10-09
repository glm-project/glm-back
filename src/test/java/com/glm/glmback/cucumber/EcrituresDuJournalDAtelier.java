package com.glm.glmback.cucumber;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Les deux ecritures du journal d'un element engage, telles que les scenarios les envoient a l'API d'atelier :
 * pointage et regularisation d'une fin.
 *
 * <p>
 * Chaque contexte qui relit ce journal y pointe par ici plutot que de composer sa propre requete : le corps d'une
 * ecriture ne change donc qu'a un seul endroit quand le contrat d'atelier evolue. Aucun etat n'est retenu, chaque
 * classe de steps garde ses propres alias.
 * </p>
 *
 * <p>
 * Le corps d'un pointage part tel que le scenario le donne : un type, un operateur, un poste, une heure. Le serveur
 * juge chaque pointage selon la regle de reception, et rien n'en est deduit ici : un scenario qui veut passer du travail
 * a la non conformite pointe une fin, puis la non conformite, a la meme heure.
 * </p>
 */
public class EcrituresDuJournalDAtelier {

  private static final String SUIVIS_URI = "/api/atelier/suivis/";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  private final CucumberRestClient rest;

  public EcrituresDuJournalDAtelier(CucumberRestClient rest) {
    this.rest = rest;
  }

  /**
   * Pointe sur un suivi, et rend la requete telle qu'elle est partie : c'est elle, a l'identique, qu'un pupitre
   * rejouerait.
   */
  public PointageEnvoye pointe(String suivi, Map<String, ?> corps) {
    return envoieA(SUIVIS_URI + suivi + "/pointages", corps);
  }

  /**
   * Regularise la fin d'une activite echue : le corps ne porte que l'activite et l'heure, et l'identifiant que le client
   * fournit, tire ici quand le scenario n'en donne pas. Rend la requete telle qu'elle est partie, de quoi la renvoyer.
   */
  public PointageEnvoye regularise(String suivi, Map<String, ?> corps) {
    Map<String, Object> complet = new HashMap<>(corps);
    complet.putIfAbsent("id", UUID.randomUUID().toString());

    return envoieA(SUIVIS_URI + suivi + "/regularisations", complet);
  }

  /**
   * Regularise exactement le corps donne, sans identifiant tire : de quoi eprouver le refus d'un corps incomplet.
   */
  public PointageEnvoye regulariseTelQuel(String suivi, Map<String, ?> corps) {
    return envoieA(SUIVIS_URI + suivi + "/regularisations", corps);
  }

  /**
   * Verifie la reponse du dernier pointage d'un tableau de mise en place : accepte par defaut, ignore par la regle de
   * reception quand la ligne porte {@code reponse = ignore}. Un pointage ignore n'entre pas au journal : le scenario le
   * dit, plutot que de le laisser changer en silence ce qu'il raconte.
   */
  public static void exigeLaReponseAttendue(Map<String, String> pointage) {
    if ("ignore".equals(pointage.get("reponse"))) {
      assertThat(CucumberRestTestContext.getStatus().value()).as("le pointage doit etre ignore").isEqualTo(409);
      assertThat(CucumberRestTestContext.getElement("$.type")).isEqualTo("urn:glm:erreur:atelier:pointage-ignore");
    } else {
      assertThat(CucumberRestTestContext.getStatus().is2xxSuccessful()).as("le pointage doit etre accepte").isTrue();
    }
  }

  private PointageEnvoye envoieA(String uri, Map<String, ?> corps) {
    PointageEnvoye envoye = new PointageEnvoye(uri, JSON.writeValueAsString(corps));
    rest.post(envoye.uri(), envoye.corps());

    return envoye;
  }

  public record PointageEnvoye(String uri, String corps) {}
}

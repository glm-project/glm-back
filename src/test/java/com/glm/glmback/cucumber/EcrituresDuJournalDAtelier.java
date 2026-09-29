package com.glm.glmback.cucumber;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Les trois ecritures du journal d'un element engage, telles que les scenarios les envoient a l'API d'atelier :
 * pointage, regularisation et correction.
 *
 * <p>
 * Chaque contexte qui relit ce journal y pointe par ici plutot que de composer sa propre requete : le corps d'une
 * ecriture ne change donc qu'a un seul endroit quand le contrat d'atelier evolue. Aucun etat n'est retenu, chaque
 * classe de steps garde ses propres alias.
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
    PointageEnvoye pointage = new PointageEnvoye(SUIVIS_URI + suivi + "/pointages", JSON.writeValueAsString(corps));
    rest.post(pointage.uri(), pointage.corps());

    return pointage;
  }

  public void regularise(String suivi, Map<String, ?> corps) {
    rest.post(SUIVIS_URI + suivi + "/regularisations", JSON.writeValueAsString(corps));
  }

  public void corrige(String suivi, String evenement, Map<String, ?> corps) {
    rest.put(SUIVIS_URI + suivi + "/evenements/" + evenement, JSON.writeValueAsString(corps));
  }

  public record PointageEnvoye(String uri, String corps) {}
}

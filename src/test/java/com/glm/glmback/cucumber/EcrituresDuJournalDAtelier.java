package com.glm.glmback.cucumber;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * Un corps qui porte son intention part tel quel : c'est le cas de tout scenario qui eprouve l'intention ou la cible.
 * Les autres scenarios ne racontent que des pointages : un debut ou une non conformite ouvre une activite, et une fin
 * vise l'activite ouverte en dernier sur le couple operateur/poste du geste, a son heure. Rien n'est deduit de plus : un
 * scenario qui veut passer du travail a la non conformite pointe une fin, puis l'ouverture, a la meme heure.
 * </p>
 */
public class EcrituresDuJournalDAtelier {

  private static final String SUIVIS_URI = "/api/atelier/suivis/";
  private static final ObjectMapper JSON = JsonMapper.builder().build();

  private final CucumberRestClient rest;
  private final CucumberClock horloge;

  public EcrituresDuJournalDAtelier(CucumberRestClient rest, CucumberClock horloge) {
    this.rest = rest;
    this.horloge = horloge;
  }

  /**
   * Pointe sur un suivi, et rend la requete telle qu'elle est partie : c'est elle, a l'identique, qu'un pupitre
   * rejouerait.
   */
  public PointageEnvoye pointe(String suivi, Map<String, ?> corps) {
    return envoie(suivi, avecIntention(suivi, corps));
  }

  /**
   * Pointe sur un suivi exactement le corps donne, sans rien en deduire : de quoi eprouver le refus d'un corps sans
   * intention.
   */
  public PointageEnvoye pointeTelQuel(String suivi, Map<String, ?> corps) {
    return envoie(suivi, corps);
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

  private PointageEnvoye envoie(String suivi, Map<String, ?> corps) {
    return envoieA(SUIVIS_URI + suivi + "/pointages", corps);
  }

  private PointageEnvoye envoieA(String uri, Map<String, ?> corps) {
    PointageEnvoye envoye = new PointageEnvoye(uri, JSON.writeValueAsString(corps));
    rest.post(envoye.uri(), envoye.corps());

    return envoye;
  }

  private Map<String, Object> avecIntention(String suivi, Map<String, ?> corps) {
    Map<String, Object> complet = new HashMap<>(corps);
    if (corps.containsKey("intention")) {
      return complet;
    }

    String type = String.valueOf(corps.get("type"));
    List<Map<String, Object>> precedents = precedents(suivi, corps);
    Optional<Map<String, Object>> dernierOuvrant = precedents
      .stream()
      .filter(evenement -> "OUVERTURE".equals(evenement.get("intention")))
      .reduce((premier, second) -> second);

    if ("FIN".equals(type)) {
      complet.put("intention", "FIN");
      complet.put("cible", dernierOuvrant.map(ouvrant -> ouvrant.get("activite")).orElseGet(() -> UUID.randomUUID().toString()));
    } else {
      complet.put("intention", "OUVERTURE");
    }

    return complet;
  }

  /**
   * Les pointages actifs du couple operateur/poste du geste, survenus au plus tard a son heure, dans l'ordre du
   * journal.
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> precedents(String suivi, Map<String, ?> corps) {
    rest.get(SUIVIS_URI + suivi);
    if (!CucumberRestTestContext.getStatus().is2xxSuccessful()) {
      return List.of();
    }

    Instant heure = Optional.ofNullable(corps.get("dateDeSurvenue")).map(String::valueOf).map(Instant::parse).orElseGet(horloge::instant);
    String operateur = String.valueOf(corps.get("operateur"));
    String poste = Optional.ofNullable(corps.get("poste")).map(String::valueOf).orElse(null);

    return ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.journal")).stream()
      .filter(evenement -> operateur.equals(identifiant(evenement.get("operateur"))))
      .filter(evenement -> Objects.equals(poste, identifiant(evenement.get("poste"))))
      .filter(evenement -> !Instant.parse(String.valueOf(evenement.get("dateDeSurvenue"))).isAfter(heure))
      .toList();
  }

  @SuppressWarnings("unchecked")
  private static String identifiant(Object ressource) {
    return ressource == null ? null : String.valueOf(((Map<String, Object>) ressource).get("id"));
  }

  public record PointageEnvoye(String uri, String corps) {}
}

package com.glm.glmback.cucumber;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
  private static final Set<Object> OUVRANTS = Set.of("OUVERTURE", "TRANSITION");
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
    return envoie(suivi, avecIntention(suivi, corps, Optional.empty()));
  }

  /**
   * Pointe sur un suivi exactement le corps donne, sans rien en deduire : de quoi eprouver le refus d'un corps sans
   * intention.
   */
  public PointageEnvoye pointeTelQuel(String suivi, Map<String, ?> corps) {
    return envoie(suivi, corps);
  }

  public void regularise(String suivi, Map<String, ?> corps) {
    rest.post(SUIVIS_URI + suivi + "/regularisations", JSON.writeValueAsString(avecIntention(suivi, corps, Optional.empty())));
  }

  public void corrige(String suivi, String evenement, Map<String, ?> corps) {
    rest.put(SUIVIS_URI + suivi + "/evenements/" + evenement, JSON.writeValueAsString(avecIntention(suivi, corps, Optional.of(evenement))));
  }

  private PointageEnvoye envoie(String suivi, Map<String, ?> corps) {
    PointageEnvoye pointage = new PointageEnvoye(SUIVIS_URI + suivi + "/pointages", JSON.writeValueAsString(corps));
    rest.post(pointage.uri(), pointage.corps());

    return pointage;
  }

  private Map<String, Object> avecIntention(String suivi, Map<String, ?> corps, Optional<String> corrige) {
    Map<String, Object> complet = new HashMap<>(corps);
    if (corps.containsKey("intention")) {
      return complet;
    }

    String type = String.valueOf(corps.get("type"));
    List<Map<String, Object>> precedents = precedents(suivi, corps, corrige);
    Optional<Map<String, Object>> dernierOuvrant = precedents
      .stream()
      .filter(evenement -> OUVRANTS.contains(evenement.get("intention")))
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
   * journal. Le fait corrige n'en est pas : il va etre annule.
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> precedents(String suivi, Map<String, ?> corps, Optional<String> corrige) {
    rest.get(SUIVIS_URI + suivi);
    if (!CucumberRestTestContext.getStatus().is2xxSuccessful()) {
      return List.of();
    }

    Instant heure = Optional.ofNullable(corps.get("dateDeSurvenue")).map(String::valueOf).map(Instant::parse).orElseGet(horloge::instant);
    String operateur = String.valueOf(corps.get("operateur"));
    String poste = Optional.ofNullable(corps.get("poste")).map(String::valueOf).orElse(null);

    return ((List<Map<String, Object>>) CucumberRestTestContext.getElement("$.journal")).stream()
      .filter(evenement -> evenement.get("annulation") == null)
      .filter(evenement -> corrige.filter(evenement.get("id")::equals).isEmpty())
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

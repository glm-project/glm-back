package com.glm.glmback.cucumber;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un poste ne se declare qu'avec l'identifiant d'une nature du referentiel. Les scenarios partagent la base de leur
 * entreprise : la nature est enregistree si elle manque, sinon retrouvee dans la liste, a la casse et aux accents pres.
 *
 * <p>
 * Un utilisateur qui ne peut ni l'enregistrer ni la trouver recoit un identifiant inconnu : son refus vient alors du
 * poste lui-meme, ce que ces scenarios verifient.
 * </p>
 */
public final class NaturesDesScenarios {

  private static final String NATURES_URI = "/api/natures-de-travail";

  private NaturesDesScenarios() {}

  public static String identifiant(CucumberRestClient rest, String libelle) {
    rest.post(NATURES_URI, JsonMapper.builder().build().writeValueAsString(Map.of("libelle", libelle)));
    if (CucumberRestTestContext.getStatus() == HttpStatus.CREATED) {
      return (String) CucumberRestTestContext.getElement("$.id");
    }
    rest.get(NATURES_URI + "?size=100");
    return existante(libelle);
  }

  @SuppressWarnings("unchecked")
  private static String existante(String libelle) {
    if (CucumberRestTestContext.getStatus() != HttpStatus.OK) {
      return UUID.randomUUID().toString();
    }
    List<Map<String, Object>> natures = (List<Map<String, Object>>) CucumberRestTestContext.getElement("$.content");
    return natures
      .stream()
      .filter(nature -> cle(String.valueOf(nature.get("libelle"))).equals(cle(libelle)))
      .map(nature -> String.valueOf(nature.get("id")))
      .findFirst()
      .orElseGet(() -> UUID.randomUUID().toString());
  }

  private static String cle(String libelle) {
    return Normalizer.normalize(libelle, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").strip().toLowerCase(Locale.ROOT);
  }
}

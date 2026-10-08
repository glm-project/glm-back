package com.glm.glmback.cucumber;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un element de fabrication ne se cree que dans une categorie declaree. Les scenarios partagent la base de leur
 * entreprise : la categorie est declaree si elle manque, et le refus d'un doublon est sans consequence.
 */
public final class CategoriesDeProduitDesScenarios {

  private CategoriesDeProduitDesScenarios() {}

  public static void declarer(CucumberRestClient rest, String code) {
    rest.post("/api/categories-de-produit", JsonMapper.builder().build().writeValueAsString(Map.of("code", code)));
  }
}

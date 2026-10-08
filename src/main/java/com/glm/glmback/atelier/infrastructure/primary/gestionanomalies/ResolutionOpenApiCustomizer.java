package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Jackson's subtype hierarchy makes swagger-core add a parent allOf to each alternative of a oneOf union.
 * These alternatives describe standalone JSON bodies: retaining the parent would recursively require the union.
 * Preserve their generated properties and constraints, with the literal discriminator emitted by Jackson: nature for
 * the lines of the anomalies list.
 */
@Component
final class ResolutionOpenApiCustomizer implements OpenApiCustomizer {

  private record Variante(String discriminant, String valeur) {}

  private static final Map<String, Variante> VARIANTES = Map.of(
    "RestConflitEnListe",
    new Variante("nature", "CONFLIT"),
    "RestFinAutomatiqueEnListe",
    new Variante("nature", "FIN_AUTOMATIQUE")
  );

  @Override
  public void customise(OpenAPI specification) {
    VARIANTES.forEach((nom, discriminee) -> {
      Schema variante = specification.getComponents().getSchemas().get(nom);
      Map<String, Schema> proprietes = new LinkedHashMap<>();
      for (Object partie : variante.getAllOf()) {
        var schema = (Schema) partie;
        if (schema.getProperties() != null) {
          proprietes.putAll(schema.getProperties());
        }
      }
      proprietes.put(discriminee.discriminant(), new StringSchema().addEnumItem(discriminee.valeur()));
      variante.setAllOf(null);
      variante.setType("object");
      variante.setProperties(proprietes);
      variante.addRequiredItem(discriminee.discriminant());
    });
  }
}

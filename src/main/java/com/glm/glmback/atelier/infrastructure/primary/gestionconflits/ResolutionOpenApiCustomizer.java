package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

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
 * Preserve their generated properties and constraints, with the literal discriminator emitted by Jackson.
 */
@Component
final class ResolutionOpenApiCustomizer implements OpenApiCustomizer {

  private static final Map<String, String> VARIANTES = Map.of(
    "RestActeAnnulation",
    "ANNULATION",
    "RestActeCorrection",
    "CORRECTION",
    "RestActeRegularisation",
    "REGULARISATION",
    "RestConfirmationEnregistree",
    "ENREGISTREE",
    "RestConfirmationNonAttestee",
    "NON_ATTESTEE"
  );

  @Override
  public void customise(OpenAPI specification) {
    VARIANTES.forEach((nom, kind) -> {
      Schema variante = specification.getComponents().getSchemas().get(nom);
      Map<String, Schema> proprietes = new LinkedHashMap<>();
      for (Object partie : variante.getAllOf()) {
        var schema = (Schema) partie;
        if (schema.getProperties() != null) {
          proprietes.putAll(schema.getProperties());
        }
      }
      proprietes.put("kind", new StringSchema().addEnumItem(kind));
      variante.setAllOf(null);
      variante.setType("object");
      variante.setProperties(proprietes);
      variante.addRequiredItem("kind");
    });
  }
}

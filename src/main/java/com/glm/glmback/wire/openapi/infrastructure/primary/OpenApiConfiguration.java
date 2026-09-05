package com.glm.glmback.wire.openapi.infrastructure.primary;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the OpenAPI description consumed by front-end developers on /swagger-ui.html and /v3/api-docs.
 *
 * <p>
 * The description spells out the two things a caller cannot guess from the schemas alone: every /api/** endpoint is
 * gated by the tenant claim, and business acts are opened by the GESTIONNAIRE role rather than ADMIN.
 * </p>
 *
 * <p>
 * This bean follows springdoc's own switch rather than a profile list of its own: one flag decides whether the
 * description exists at all, so the bean and the endpoints serving it can never disagree. The flag defaults to false
 * and is turned on by the local and test profiles.
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
class OpenApiConfiguration {

  private static final String BEARER_SCHEME = "bearer-jwt";

  private static final String DESCRIPTION = """
    API of the GLM manufacturing tracking application.

    ## Authentication

    Every endpoint expects a Keycloak bearer token (realm `glmproject`, issuer `http://localhost:9080/realms/glmproject`).

    The token **must carry a `tenant` claim** naming a declared company: each company owns a PostgreSQL schema, and the
    whole `/api/**` surface answers `403` to a token without a known tenant. No endpoint takes the company as a
    parameter — it is always read from the token.

    ## Roles

    - `USER` — shop-floor operator: clocks presence and work, reads.
    - `GESTIONNAIRE` — back-office: everything a `USER` can do, plus committing elements to the shop floor, closing
      them, and correcting entries (`regularise`, `annule`, `corrige`).
    - `ADMIN` — technical administration (`/api/admin/**`, `/management/**`) only. **It grants no business access.**

    Development users (password equal to the login): `gestionnaire.impeccmold`, `user.impeccmold`,
    `gestionnaire.katilys`, `user.katilys`.
    """;

  @Bean
  OpenAPI glmprojectOpenApi() {
    return new OpenAPI()
      .info(
        new Info()
          .title("glmproject API")
          .description(DESCRIPTION)
          .version("0.0.1")
          .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
      )
      .components(
        new Components().addSecuritySchemes(
          BEARER_SCHEME,
          new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Keycloak access token carrying the `tenant` claim.")
        )
      )
      .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }

  @Bean
  ModelConverter restPageModelConverter() {
    return new RestPageModelConverter();
  }
}

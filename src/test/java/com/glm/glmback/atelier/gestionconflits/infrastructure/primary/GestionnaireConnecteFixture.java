package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import static com.glm.glmback.atelier.gestionconflits.application.ResolutionFixture.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

final class GestionnaireConnecteFixture {

  private GestionnaireConnecteFixture() {}

  static OAuth2AuthenticationToken oidcAvecEmetteurUrl() throws Exception {
    var identite = new OidcIdToken(
      "token",
      Instant.parse("2026-05-10T17:00:00Z"),
      Instant.parse("2026-05-10T18:00:00Z"),
      Map.of(
        "iss",
        URI.create(EMETTEUR_GLM).toURL(),
        "sub",
        SUJET_LEROY,
        "preferred_username",
        GESTIONNAIRE_LEROY.auteur().value(),
        "tenant",
        TENANT_IMPECCMOLD
      )
    );
    return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), identite), List.of(), "keycloak");
  }
}

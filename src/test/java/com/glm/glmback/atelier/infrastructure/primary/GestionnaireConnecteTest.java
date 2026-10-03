package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@UnitTest
class GestionnaireConnecteTest {

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldLireLIdentiteStableEtLAuteurDepuisLeJwt() {
    // GIVEN
    Jwt jwt = Jwt.withTokenValue("token")
      .header("alg", "RS256")
      .issuer(EMETTEUR_GLM)
      .subject(SUJET_LEROY)
      .claim("preferred_username", GESTIONNAIRE_LEROY.auteur().value())
      .claim("tenant", TENANT_IMPECCMOLD)
      .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    // WHEN
    var gestionnaire = GestionnaireConnecte.get();
    // THEN
    assertThat(gestionnaire).isEqualTo(CONTEXTE_LEROY_IMPECCMOLD);
  }
}

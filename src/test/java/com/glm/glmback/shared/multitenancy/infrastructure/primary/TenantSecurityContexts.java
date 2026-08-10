package com.glm.glmback.shared.multitenancy.infrastructure.primary;

import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Construit un contexte de securite porteur d'un claim de tenant, seule forme d'authentification qui
 * ouvre l'acces a la base.
 */
public final class TenantSecurityContexts {

  private static final List<String> ROLES = List.of("ROLE_ADMIN", "ROLE_USER");

  private TenantSecurityContexts() {}

  public static void authenticateOn(String tenant) {
    SecurityContextHolder.setContext(securityContext(tenant));
  }

  static SecurityContext securityContext(String tenant) {
    Jwt jwt = Jwt.withTokenValue("token")
      .header("alg", JwsAlgorithms.RS256)
      .subject("admin")
      .claim("preferred_username", "admin")
      .claim("groups", ROLES)
      .claim(CurrentTenant.TENANT, tenant)
      .build();

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new JwtAuthenticationToken(jwt, ROLES.stream().map(SimpleGrantedAuthority::new).toList()));

    return context;
  }
}

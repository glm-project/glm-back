package com.glm.glmback.shared.multitenancy.application;

import com.glm.glmback.shared.multitenancy.domain.Tenant;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class CurrentTenant {

  public static final String TENANT = "tenant";

  private CurrentTenant() {}

  public static Tenant tenant() {
    return optionalTenant().orElseThrow(NotTenantedUserException::new);
  }

  public static Optional<Tenant> optionalTenant() {
    return authentication().flatMap(CurrentTenant::readTenant);
  }

  private static Optional<Tenant> readTenant(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken token) {
      return Tenant.of((String) token.getToken().getClaims().get(TENANT));
    }

    if (authentication.getPrincipal() instanceof DefaultOidcUser oidcUser) {
      return Tenant.of((String) oidcUser.getAttributes().get(TENANT));
    }

    return Optional.empty();
  }

  private static Optional<Authentication> authentication() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
  }
}

package com.glm.glmback.shared.multitenancy.infrastructure.primary;

import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import com.glm.glmback.shared.multitenancy.domain.Tenants;
import java.util.function.Supplier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * Sans entreprise connue, pas de donnees : la verification est faite ici, avant l'ouverture de la
 * transaction, sans quoi l'echec de resolution du schema remonterait en erreur technique.
 */
@Component
public class TenantAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

  private final Tenants tenants;

  public TenantAuthorizationManager(Tenants tenants) {
    this.tenants = tenants;
  }

  @Override
  public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
    return new AuthorizationDecision(isAuthenticated(authentication.get()) && hasKnownTenant());
  }

  private static boolean isAuthenticated(Authentication authentication) {
    return authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
  }

  private boolean hasKnownTenant() {
    return CurrentTenant.optionalTenant().filter(tenants::contains).isPresent();
  }
}

package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * L'identifiant de tenant vu par Hibernate est la cle de l'entreprise ; c'est le registre qui en donne le
 * schema, et {@link TenantDataSources} le pool. Une entreprise inconnue du registre echoue des l'ouverture
 * de session, et non au fond de l'acquisition de connexion.
 */
@Component
class CurrentTenantResolver implements CurrentTenantIdentifierResolver<String> {

  /** Hors requete : pool principal et schema par defaut. Ne respecte pas le motif d'une cle d'entreprise. */
  static final String OUT_OF_REQUEST = "_out_of_request";

  private final TenantRegistry tenantRegistry;

  CurrentTenantResolver(TenantRegistry tenantRegistry) {
    this.tenantRegistry = tenantRegistry;
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    if (RequestContextHolder.getRequestAttributes() == null) {
      return OUT_OF_REQUEST;
    }

    Tenant tenant = CurrentTenant.tenant();
    if (!tenantRegistry.contains(tenant)) {
      throw new NotTenantedUserException();
    }

    return tenant.value();
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}

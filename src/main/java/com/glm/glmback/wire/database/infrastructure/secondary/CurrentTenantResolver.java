package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * L'identifiant de tenant vu par Hibernate est le nom du schema PostgreSQL : un tenant inconnu de la
 * configuration echoue donc des l'ouverture de session, et non au fond de l'acquisition de connexion.
 */
@Component
class CurrentTenantResolver implements CurrentTenantIdentifierResolver<String> {

  private final TenantSchemas tenantSchemas;

  CurrentTenantResolver(TenantSchemas tenantSchemas) {
    this.tenantSchemas = tenantSchemas;
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    if (RequestContextHolder.getRequestAttributes() == null) {
      return tenantSchemas.defaultSchema();
    }

    return tenantSchemas.schema(CurrentTenant.tenant());
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}

package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.error.domain.Assert;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.shared.multitenancy.domain.Tenants;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Les entreprises connues sont les lignes actives de la table {@code tenant}, lues une fois au demarrage.
 * Une entreprise ajoutee ou suspendue n'est donc prise en compte qu'au redemarrage suivant.
 */
@Component
class TenantRegistry implements Tenants {

  private final Map<Tenant, String> schemas;
  private final String defaultSchema;

  @Autowired
  TenantRegistry(AdminSchemaInitializer adminSchema, DataSource dataSource) {
    this(adminSchema.schema(), activeTenants(adminSchema.schema(), dataSource));
  }

  TenantRegistry(String defaultSchema, Collection<TenantDeclaration> tenants) {
    this.defaultSchema = defaultSchema;
    schemas = tenants.stream().collect(Collectors.toUnmodifiableMap(tenant -> new Tenant(tenant.id()), TenantRegistry::schemaOf));
  }

  private static List<TenantDeclaration> activeTenants(String adminSchema, DataSource dataSource) {
    return JdbcClient.create(dataSource)
      .sql("SELECT id, schema_name FROM \"%s\".tenant WHERE status = 'ACTIVE'".formatted(adminSchema))
      .query((row, index) -> new TenantDeclaration(row.getString("id"), row.getString("schema_name")))
      .list();
  }

  @Override
  public boolean contains(Tenant tenant) {
    return schemas.containsKey(tenant);
  }

  String schema(Tenant tenant) {
    return Optional.ofNullable(schemas.get(tenant)).orElseThrow(NotTenantedUserException::new);
  }

  /**
   * Schema utilise hors requete, ou aucun tenant n'est resolvable : il ne porte aucune table metier,
   * une lecture qui y atterrirait echouerait donc bruyamment plutot que de rendre les donnees d'autrui.
   */
  String defaultSchema() {
    return defaultSchema;
  }

  Collection<String> schemas() {
    return schemas.values();
  }

  Set<Tenant> tenants() {
    return schemas.keySet();
  }

  /** Schema d'un identifiant de tenant Hibernate, tel que le rend {@link CurrentTenantResolver}. */
  String schemaName(String tenantIdentifier) {
    if (CurrentTenantResolver.OUT_OF_REQUEST.equals(tenantIdentifier)) {
      return defaultSchema;
    }

    return schema(new Tenant(tenantIdentifier));
  }

  private static String schemaOf(TenantDeclaration tenant) {
    Assert.field("schema of tenant " + tenant.id(), tenant.schema()).notBlank().matches(SchemaNames.PATTERN);

    return tenant.schema();
  }

  record TenantDeclaration(String id, String schema) {}
}

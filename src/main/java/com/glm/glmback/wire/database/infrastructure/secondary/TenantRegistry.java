package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.error.domain.Assert;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.shared.multitenancy.domain.Tenants;
import com.glm.glmback.wire.database.infrastructure.secondary.TenantDeclaration.DatabaseAccess;
import com.glm.glmback.wire.database.infrastructure.secondary.TenantDeclaration.PoolSettings;
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

  private final Map<Tenant, TenantDeclaration> declarations;
  private final String defaultSchema;

  @Autowired
  TenantRegistry(AdminSchemaInitializer adminSchema, DataSource dataSource) {
    this(adminSchema.schema(), activeTenants(adminSchema.schema(), dataSource));
  }

  TenantRegistry(String defaultSchema, Collection<TenantDeclaration> tenants) {
    this.defaultSchema = defaultSchema;
    declarations = tenants.stream().collect(Collectors.toUnmodifiableMap(tenant -> new Tenant(tenant.id()), TenantRegistry::validated));
  }

  private static List<TenantDeclaration> activeTenants(String adminSchema, DataSource dataSource) {
    return JdbcClient.create(dataSource)
      .sql(
        "SELECT id, schema_name, jdbc_url, username, secret_ref, pool_max_size FROM \"%s\".tenant WHERE status = 'ACTIVE'".formatted(
          adminSchema
        )
      )
      .query((row, index) ->
        new TenantDeclaration(
          row.getString("id"),
          row.getString("schema_name"),
          new PoolSettings(
            new DatabaseAccess(
              Optional.ofNullable(row.getString("jdbc_url")),
              Optional.ofNullable(row.getString("username")),
              Optional.ofNullable(row.getString("secret_ref"))
            ),
            Optional.ofNullable(row.getObject("pool_max_size", Integer.class))
          )
        )
      )
      .list();
  }

  @Override
  public boolean contains(Tenant tenant) {
    return declarations.containsKey(tenant);
  }

  String schema(Tenant tenant) {
    return declaration(tenant).schema();
  }

  TenantDeclaration declaration(Tenant tenant) {
    return Optional.ofNullable(declarations.get(tenant)).orElseThrow(NotTenantedUserException::new);
  }

  /**
   * Schema utilise hors requete, ou aucun tenant n'est resolvable : il ne porte aucune table metier,
   * une lecture qui y atterrirait echouerait donc bruyamment plutot que de rendre les donnees d'autrui.
   */
  String defaultSchema() {
    return defaultSchema;
  }

  Set<Tenant> tenants() {
    return declarations.keySet();
  }

  /** Schema d'un identifiant de tenant Hibernate, tel que le rend {@link CurrentTenantResolver}. */
  String schemaName(String tenantIdentifier) {
    if (CurrentTenantResolver.OUT_OF_REQUEST.equals(tenantIdentifier)) {
      return defaultSchema;
    }

    return schema(new Tenant(tenantIdentifier));
  }

  private static TenantDeclaration validated(TenantDeclaration tenant) {
    Assert.field("schema of tenant " + tenant.id(), tenant.schema()).notBlank().matches(SchemaNames.PATTERN);

    return tenant;
  }
}

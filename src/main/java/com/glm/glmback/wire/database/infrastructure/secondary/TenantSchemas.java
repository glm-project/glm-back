package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.error.domain.Assert;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.shared.multitenancy.domain.Tenants;
import com.glm.glmback.wire.database.infrastructure.secondary.MultitenancyProperties.TenantProperties;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class TenantSchemas implements Tenants {

  private static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

  private final Map<Tenant, String> schemas;
  private final String defaultSchema;

  TenantSchemas(MultitenancyProperties properties) {
    defaultSchema = validated("default-schema", properties.getDefaultSchema());
    schemas = properties
      .getTenants()
      .stream()
      .collect(Collectors.toUnmodifiableMap(tenant -> new Tenant(tenant.getId()), TenantSchemas::schemaOf));
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

  private static String schemaOf(TenantProperties tenant) {
    return validated("schema", tenant.getSchema());
  }

  private static String validated(String champ, String schema) {
    Assert.field(champ, schema).notBlank().matches(SCHEMA_PATTERN);

    return schema;
  }
}

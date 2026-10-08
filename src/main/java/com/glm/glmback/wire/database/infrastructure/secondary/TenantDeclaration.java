package com.glm.glmback.wire.database.infrastructure.secondary;

import java.util.Optional;

/** Une ligne active du registre : la cle de l'entreprise, son schema et ce que son pool change au pool principal. */
record TenantDeclaration(String id, String schema, PoolSettings pool) {
  TenantDeclaration(String id, String schema) {
    this(id, schema, PoolSettings.INHERITED);
  }

  /** Une valeur absente reprend celle du pool principal. */
  record PoolSettings(DatabaseAccess database, Optional<Integer> maximumSize) {
    static final PoolSettings INHERITED = new PoolSettings(DatabaseAccess.MAIN, Optional.empty());
  }

  /**
   * Base visee par le pool. {@code secretRef} est le nom de la variable d'environnement qui porte le mot de
   * passe, jamais le mot de passe lui-meme.
   */
  record DatabaseAccess(Optional<String> jdbcUrl, Optional<String> username, Optional<String> secretRef) {
    static final DatabaseAccess MAIN = new DatabaseAccess(Optional.empty(), Optional.empty(), Optional.empty());
  }
}

package com.glm.glmback.wire.database.infrastructure.secondary;

import java.util.regex.Pattern;

final class SchemaNames {

  static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

  private SchemaNames() {}
}

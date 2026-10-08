package com.glm.glmback.wire.database.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@UnitTest
class AdminSchemaInitializerTest {

  private final DataSource dataSource = mock(DataSource.class);

  @Test
  void shouldDefaultToPublicSchema() {
    assertThat(new AdminSchemaInitializer(dataSource, new MultitenancyProperties()).schema()).isEqualTo("public");
  }

  @Test
  void shouldNotBuildWithDefaultSchemaOutOfPattern() {
    MultitenancyProperties properties = new MultitenancyProperties();
    properties.setDefaultSchema("Public Schema");

    assertThatThrownBy(() -> new AdminSchemaInitializer(dataSource, properties))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("default-schema");
  }
}

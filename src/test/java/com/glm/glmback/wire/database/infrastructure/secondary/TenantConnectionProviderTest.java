package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantConnectionProviderTest {

  private final DataSource mainDataSource = mock(DataSource.class);
  private final TenantDataSources tenantDataSources = mock(TenantDataSources.class);
  private final TenantConnectionProvider provider = new TenantConnectionProvider(mainDataSource, tenantDataSources);

  @Test
  void shouldUseTheTenantPool() {
    HikariDataSource katilys = mock(HikariDataSource.class);
    when(tenantDataSources.dataSource(TENANT_KATILYS)).thenReturn(katilys);

    assertThat(provider.selectDataSource("katilys")).isSameAs(katilys);
  }

  @Test
  void shouldUseTheMainPoolOutOfRequest() {
    assertThat(provider.selectDataSource(CurrentTenantResolver.OUT_OF_REQUEST)).isSameAs(mainDataSource);
    verifyNoInteractions(tenantDataSources);
  }

  @Test
  void shouldUseTheMainPoolForHibernateOwnNeeds() {
    assertThat(provider.selectAnyDataSource()).isSameAs(mainDataSource);
  }
}

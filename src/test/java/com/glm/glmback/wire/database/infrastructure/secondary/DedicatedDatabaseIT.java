package com.glm.glmback.wire.database.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateurRepository;
import com.glm.glmback.operateur.domain.OperateursFixture;
import com.glm.glmback.shared.authentication.infrastructure.primary.TestSecurityConfiguration;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A tenant on its own PostgreSQL server, next to a tenant of the main database. Both servers are new and not reused. */
class DedicatedDatabaseIT {

  private static final String SECRET_REF = "GLM_TENANT_DEDIEE_DB_PASSWORD";

  @Test
  void shouldMigrateAndServeATenantOnItsDedicatedDatabase() throws Exception {
    try (
      PostgreSQLContainer main = new PostgreSQLContainer("postgres:18.4").withReuse(false);
      PostgreSQLContainer dedicated = new PostgreSQLContainer("postgres:18.4")
        .withReuse(false)
        .withDatabaseName("dediee")
        .withUsername("dediee")
        .withPassword("secret-dediee")
    ) {
      main.start();
      dedicated.start();
      declare(main, dedicated);

      try (ConfigurableApplicationContext application = start(main, "--" + SECRET_REF + "=secret-dediee")) {
        assertThat(query(dedicated, "SELECT nspname FROM pg_namespace WHERE nspname = 'dediee'")).containsExactly("dediee");
        assertThat(query(main, "SELECT nspname FROM pg_namespace WHERE nspname IN ('locale', 'dediee')")).containsExactly("locale");
        assertThat(application.getBean(TenantDataSources.class).dataSource(new Tenant("dediee")).getMaximumPoolSize()).isEqualTo(3);

        createOperator(application, "dediee");
        createOperator(application, "locale");

        assertThat(query(dedicated, "SELECT count(*) FROM dediee.operateur")).containsExactly("1");
        assertThat(query(main, "SELECT count(*) FROM locale.operateur")).containsExactly("1");
      }
    }
  }

  @Test
  void shouldNotStartWhenTheSecretOfADedicatedDatabaseIsMissing() throws Exception {
    try (
      PostgreSQLContainer main = new PostgreSQLContainer("postgres:18.4").withReuse(false);
      PostgreSQLContainer dedicated = new PostgreSQLContainer("postgres:18.4").withReuse(false)
    ) {
      main.start();
      dedicated.start();
      declare(main, dedicated);

      assertThatThrownBy(() -> start(main))
        .hasStackTraceContaining("dediee")
        .hasStackTraceContaining(SECRET_REF);
      assertThat(query(dedicated, "SELECT nspname FROM pg_namespace WHERE nspname = 'dediee'")).isEmpty();
    }
  }

  private static void declare(PostgreSQLContainer main, PostgreSQLContainer dedicated) throws Exception {
    LiquibaseMigration.migrate(
      new DriverManagerDataSource(main.getJdbcUrl(), main.getUsername(), main.getPassword()),
      "classpath:config/liquibase/admin/master.xml",
      "public"
    );
    execute(main, "INSERT INTO public.tenant (id, schema_name, status) VALUES ('locale', 'locale', 'ACTIVE')");
    execute(
      main,
      "INSERT INTO public.tenant (id, schema_name, jdbc_url, username, secret_ref, pool_max_size, status) VALUES ('dediee', 'dediee', '%s', '%s', '%s', 3, 'ACTIVE')".formatted(
        dedicated.getJdbcUrl(),
        dedicated.getUsername(),
        SECRET_REF
      )
    );
  }

  private static ConfigurableApplicationContext start(PostgreSQLContainer main, String... extraArguments) {
    List<String> arguments = new ArrayList<>(
      List.of(
        "--server.port=0",
        "--spring.datasource.url=" + main.getJdbcUrl(),
        "--spring.datasource.username=" + main.getUsername(),
        "--spring.datasource.password=" + main.getPassword(),
        "--spring.datasource.driver-class-name=org.postgresql.Driver",
        "--application.multitenancy.seed-change-log="
      )
    );
    arguments.addAll(List.of(extraArguments));
    SpringApplication application = new SpringApplication(GlmprojectApp.class, TestSecurityConfiguration.class);
    application.setAdditionalProfiles("test");
    return application.run(arguments.toArray(String[]::new));
  }

  private static void createOperator(ConfigurableApplicationContext application, String tenant) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    TenantSecurityContexts.authenticateOn(tenant);
    try {
      application
        .getBean(TransactionTemplate.class)
        .executeWithoutResult(transaction ->
          application
            .getBean(OperateurRepository.class)
            .create(OperateursFixture.operateurDeRejeuSansPoste(new OperateurId(UUID.randomUUID())))
        );
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }

  private static List<String> query(PostgreSQLContainer database, String sql) throws SQLException {
    try (
      Connection connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
      Statement statement = connection.createStatement();
      var rows = statement.executeQuery(sql)
    ) {
      List<String> result = new ArrayList<>();
      while (rows.next()) {
        result.add(rows.getString(1));
      }
      return result;
    }
  }

  private static void execute(PostgreSQLContainer database, String sql) throws SQLException {
    try (
      Connection connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
      Statement statement = connection.createStatement()
    ) {
      statement.execute(sql);
    }
  }
}

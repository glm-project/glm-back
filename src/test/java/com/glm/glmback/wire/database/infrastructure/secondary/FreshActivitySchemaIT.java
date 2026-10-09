package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduitRepository;
import com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture;
import com.glm.glmback.categoriedeproduit.domain.Rang;
import com.glm.glmback.coutderevient.application.CoutsDeRevientApplicationService;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture;
import com.glm.glmback.feuilledetemps.application.FeuillesDeTempsApplicationService;
import com.glm.glmback.operateur.domain.OperateurRepository;
import com.glm.glmback.operateur.domain.OperateursFixture;
import com.glm.glmback.pupitre.application.ReferentielsDuPupitreApplicationService;
import com.glm.glmback.shared.authentication.infrastructure.primary.TestSecurityConfiguration;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.syntheseheures.application.SynthesesDesHeuresApplicationService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Each case owns a new, non-reused database, independent of the other integration tests. */
class FreshActivitySchemaIT {

  private static final List<String> BUSINESS_TABLES = List.of(
    "activite_d_atelier",
    "categorie_de_produit",
    "compteur_d_elements_de_fabrication",
    "element_de_fabrication",
    "evenement_d_atelier",
    "operateur",
    "operateur_poste",
    "parametrage",
    "pointage_ignore_d_atelier",
    "poste_de_travail",
    "suivi_d_atelier"
  );

  @Test
  void shouldInstallTwoTenantsRestartWithoutChangingTheirDataThenInstallAThird() throws Exception {
    try (PostgreSQLContainer database = new PostgreSQLContainer("postgres:18.4").withReuse(false)) {
      database.start();
      assertThat(query(database, "SELECT nspname FROM pg_namespace WHERE nspname IN ('alpha', 'beta', 'gamma')")).isEmpty();
      Map<String, SuiviDAtelierId> followups;
      Snapshot alpha;
      Snapshot beta;
      try (ConfigurableApplicationContext application = start(database, "alpha", "beta")) {
        assertRealJpa(application);
        assertFreshSchema(database, "alpha");
        assertFreshSchema(database, "beta");
        followups = Map.of("alpha", createActivity(application, "alpha"), "beta", createActivity(application, "beta"));
        assertReaders(application, "alpha", followups.get("alpha"));
        assertReaders(application, "beta", followups.get("beta"));
        alpha = snapshot(database, "alpha");
        beta = snapshot(database, "beta");
      }
      try (ConfigurableApplicationContext application = start(database, "alpha", "beta")) {
        assertThat(snapshot(database, "alpha")).isEqualTo(alpha);
        assertThat(snapshot(database, "beta")).isEqualTo(beta);
        assertReaders(application, "alpha", followups.get("alpha"));
        assertReaders(application, "beta", followups.get("beta"));
      }
      assertThat(query(database, "SELECT nspname FROM pg_namespace WHERE nspname = 'gamma'")).isEmpty();
      try (ConfigurableApplicationContext application = start(database, "alpha", "beta", "gamma")) {
        assertFreshSchema(database, "gamma");
        assertReaders(application, "gamma", createActivity(application, "gamma"));
        assertThat(snapshot(database, "alpha")).isEqualTo(alpha);
        assertThat(snapshot(database, "beta")).isEqualTo(beta);
      }
      assertThat(tables(database, "public")).containsExactly("databasechangelog", "databasechangeloglock", "tenant");
      assertThat(query(database, "SELECT id FROM tenant")).containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }
  }

  @Test
  void shouldRejectAValidHistoricalPrefixBeforeBusinessDdlDespiteAnotherTenantsFreshMarker() throws Exception {
    try (PostgreSQLContainer database = new PostgreSQLContainer("postgres:18.4").withReuse(false)) {
      database.start();
      try (ConfigurableApplicationContext application = start(database, "marker")) {
        assertFreshSchema(database, "marker");
        assertReaders(application, "marker", createActivity(application, "marker"));
      }
      Snapshot marker = snapshot(database, "marker");
      execute(database, "CREATE SCHEMA historical");
      SpringLiquibase historical = new SpringLiquibase();
      historical.setDataSource(new DriverManagerDataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword()));
      historical.setResourceLoader(new DefaultResourceLoader());
      historical.setDefaultSchema("historical");
      historical.setLiquibaseSchema("historical");
      historical.setChangeLog("classpath:config/liquibase/historical-prefix.xml");
      historical.afterPropertiesSet();
      assertThat(query(database, "SELECT id FROM historical.databasechangelog ORDER BY orderexecuted")).containsExactly(
        "element_de_fabrication",
        "compteur_d_elements_de_fabrication"
      );
      assertThat(query(database, "SELECT filename FROM historical.databasechangelog")).containsOnly(
        "config/liquibase/changelog/2026/08/001-element_de_fabrication.xml"
      );
      assertThat(query(database, "SELECT md5sum FROM historical.databasechangelog")).allSatisfy(checksum ->
        assertThat(checksum).isNotBlank()
      );
      Snapshot previous = snapshot(database, "historical");

      assertThatThrownBy(() -> start(database, "historical", "marker"))
        .hasStackTraceContaining("initialisation_schema_neuf")
        .hasStackTraceContaining("SQL Precondition failed")
        .hasStackTraceContaining("got '2'");

      assertThat(snapshot(database, "historical")).isEqualTo(previous);
      assertThat(snapshot(database, "marker")).isEqualTo(marker);
      assertThat(tables(database, "historical")).containsExactlyInAnyOrder(
        "databasechangelog",
        "databasechangeloglock",
        "element_de_fabrication",
        "compteur_d_elements_de_fabrication"
      );
    }
  }

  @Test
  void shouldFailNormalCreationRatherThanAdoptBusinessTablesWithoutHistory() throws Exception {
    try (PostgreSQLContainer database = new PostgreSQLContainer("postgres:18.4").withReuse(false)) {
      database.start();
      execute(database, "CREATE SCHEMA unmanaged");
      execute(database, "CREATE TABLE unmanaged.element_de_fabrication (marker text NOT NULL)");
      execute(database, "INSERT INTO unmanaged.element_de_fabrication VALUES ('keep')");
      List<String> columns = query(
        database,
        "SELECT column_name || ':' || data_type FROM information_schema.columns WHERE table_schema = 'unmanaged' ORDER BY ordinal_position"
      );

      assertThatThrownBy(() -> start(database, "unmanaged"))
        .hasStackTraceContaining("element_de_fabrication")
        .hasStackTraceContaining("already exists");

      assertThat(query(database, "SELECT marker FROM unmanaged.element_de_fabrication")).containsExactly("keep");
      assertThat(
        query(
          database,
          "SELECT column_name || ':' || data_type FROM information_schema.columns WHERE table_schema = 'unmanaged' AND table_name = 'element_de_fabrication' ORDER BY ordinal_position"
        )
      ).isEqualTo(columns);
      assertThat(query(database, "SELECT id || ':' || exectype FROM unmanaged.databasechangelog ORDER BY orderexecuted")).containsExactly(
        "initialisation_schema_neuf:EXECUTED"
      );
      assertThat(tables(database, "unmanaged")).containsExactlyInAnyOrder(
        "element_de_fabrication",
        "databasechangelog",
        "databasechangeloglock"
      );
    }
  }

  private static ConfigurableApplicationContext start(PostgreSQLContainer database, String... tenants) throws Exception {
    declare(database, tenants);
    List<String> arguments = new ArrayList<>(
      List.of(
        "--server.port=0",
        "--spring.datasource.url=" + database.getJdbcUrl(),
        "--spring.datasource.username=" + database.getUsername(),
        "--spring.datasource.password=" + database.getPassword(),
        "--spring.datasource.driver-class-name=org.postgresql.Driver",
        "--application.multitenancy.seed-change-log="
      )
    );
    SpringApplication application = new SpringApplication(GlmprojectApp.class, TestSecurityConfiguration.class);
    application.setAdditionalProfiles("test");
    return application.run(arguments.toArray(String[]::new));
  }

  /** The registry is the only source of tenants: each one is declared there before the application starts. */
  private static void declare(PostgreSQLContainer database, String... tenants) throws Exception {
    LiquibaseMigration.migrate(
      new DriverManagerDataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword()),
      "classpath:config/liquibase/admin/master.xml",
      "public"
    );
    for (String tenant : tenants) {
      execute(
        database,
        "INSERT INTO public.tenant (id, schema_name, status) VALUES ('%s', '%s', 'ACTIVE') ON CONFLICT (id) DO NOTHING".formatted(
          tenant,
          tenant
        )
      );
    }
  }

  private static void assertRealJpa(ConfigurableApplicationContext application) {
    assertThat(application.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("none");
    assertThat(application.getBean(EntityManagerFactory.class).getMetamodel().getEntities()).allSatisfy(entity -> {
      Table table = entity.getJavaType().getAnnotation(Table.class);
      assertThat(table).isNotNull();
      assertThat(table.name()).isIn(BUSINESS_TABLES);
    });
  }

  private static void assertFreshSchema(PostgreSQLContainer database, String schema) throws SQLException {
    List<String> expectedTables = new ArrayList<>(BUSINESS_TABLES);
    expectedTables.addAll(List.of("databasechangelog", "databasechangeloglock"));
    assertThat(tables(database, schema)).containsExactlyInAnyOrderElementsOf(expectedTables);
    List<String> history = query(database, "SELECT id || ':' || exectype FROM \"" + schema + "\".databasechangelog ORDER BY orderexecuted");
    assertThat(history.getFirst()).isEqualTo("initialisation_schema_neuf:EXECUTED");
    assertThat(history).containsOnlyOnce("initialisation_schema_neuf:EXECUTED").contains("pointage_ignore_d_atelier:EXECUTED");
    assertThat(history).noneMatch(entry -> entry.matches("(?s).*(conflit|a_resoudre|fin_au_plus_tard|remplacement|identite_evenement).*"));
    assertThat(
      query(database, "SELECT filename FROM \"" + schema + "\".databasechangelog WHERE id = 'pointage_ignore_d_atelier'")
    ).containsExactly("config/liquibase/changelog/2026/10/013-pointage_ignore_d_atelier.xml");
    assertThat(
      query(
        database,
        "SELECT table_name || '.' || column_name || ':' || is_nullable FROM information_schema.columns WHERE table_schema = '"
          + schema
          + "'"
      )
    ).contains(
      "evenement_d_atelier.origine:NO",
      "evenement_d_atelier.activite_id:YES",
      "evenement_d_atelier.activite_visee_id:YES",
      "evenement_d_atelier.cout_horaire:YES",
      "evenement_d_atelier.taux_horaire:YES",
      "activite_d_atelier.echeance:NO"
    );
    assertThat(
      query(database, "SELECT table_name || '.' || column_name FROM information_schema.columns WHERE table_schema = '" + schema + "'")
    ).doesNotContain(
      "evenement_d_atelier.intention",
      "evenement_d_atelier.annulation_auteur",
      "evenement_d_atelier.annulation_date",
      "evenement_d_atelier.annulation_motif",
      "evenement_d_atelier.remplace_evenement_id",
      "activite_d_atelier.a_resoudre",
      "activite_d_atelier.fin_au_plus_tard",
      "activite_d_atelier.sequence_id",
      "activite_d_atelier.ordre_dans_sequence"
    );
    assertThat(query(database, "SELECT indexname FROM pg_indexes WHERE schemaname = '" + schema + "'")).contains(
      "ux_operateur_identite",
      "ux_operateur_identifiant",
      "ix_operateur_poste_poste",
      "ix_activite_d_atelier_suivi",
      "ix_activite_d_atelier_operateur"
    );
    assertThat(
      query(
        database,
        "SELECT conname FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace WHERE n.nspname = '" + schema + "'"
      )
    ).contains("pk_operateur_poste", "fk_activite_d_atelier_suivi");
  }

  private static SuiviDAtelierId createActivity(ConfigurableApplicationContext application, String tenant) {
    return onTenant(tenant, () -> {
      TransactionTemplate transactions = application.getBean(TransactionTemplate.class);
      var followup = suiviDAtelierEngage();
      transactions.executeWithoutResult(transaction -> {
        application
          .getBean(OperateurRepository.class)
          .create(
            OperateursFixture.operateurDeRejeuSansPoste(new com.glm.glmback.operateur.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()))
          );
        CategorieDeProduitRepository categories = application.getBean(CategorieDeProduitRepository.class);
        if (categories.get(CategoriesDeProduitFixture.CODE_OF).isEmpty()) {
          categories.create(new CategorieDeProduit(CategoriesDeProduitFixture.CODE_OF, Rang.premier()));
        }
        application
          .getBean(ElementDeFabricationRepository.class)
          .create(ElementsDeFabricationFixture.elementDeFabricationOrdre1015(new ElementDeFabricationId(ELEMENT_OF_2026_000042.uuid())));
        application.getBean(SuiviDAtelierRepository.class).create(followup);
      });
      var atelier = application.getBean(SuivisDAtelierApplicationService.class);
      var opening = PointageAEnregistrer.pupitreBuilder()
        .suivi(followup.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.empty())
        .auteur(AUTEUR_DUPONT)
        .dateDeSurvenue(Optional.of(LE_10_MAI_2026_A_8H))
        .evenement(EvenementDAtelierId.newId());
      atelier.pointeDuPupitre(opening);
      var finish = PointageAEnregistrer.pupitreBuilder()
        .suivi(followup.id())
        .type(TypeDEvenementDAtelier.FIN)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.empty())
        .auteur(AUTEUR_DUPONT)
        .dateDeSurvenue(Optional.of(LE_10_MAI_2026_A_17H))
        .evenement(EvenementDAtelierId.newId());
      assertThat(atelier.pointeDuPupitre(finish).rejeu()).isFalse();
      assertThat(atelier.pointeDuPupitre(finish).rejeu()).isTrue();
      return followup.id();
    });
  }

  private static void assertReaders(ConfigurableApplicationContext application, String tenant, SuiviDAtelierId followup) {
    onTenant(tenant, () -> {
      var atelier = application.getBean(SuivisDAtelierApplicationService.class);
      assertThat(atelier.get(followup).suivi().journal().evenements()).hasSize(2);
      assertThat(atelier.get(followup).suivi().activites()).hasSize(1);
      var sheet = application
        .getBean(FeuillesDeTempsApplicationService.class)
        .historique(
          new com.glm.glmback.feuilledetemps.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()),
          new com.glm.glmback.feuilledetemps.domain.SemaineCalendaire(2026, 19),
          Optional.of(LE_11_MAI_2026_A_9H15)
        );
      assertThat(sheet.jours()).hasSize(7);
      assertThat(
        sheet
          .jours()
          .stream()
          .flatMap(day -> day.activites().stream())
          .toList()
      ).hasSize(1);
      var hours = application
        .getBean(SynthesesDesHeuresApplicationService.class)
        .synthese(
          new com.glm.glmback.syntheseheures.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()),
          new com.glm.glmback.syntheseheures.domain.SemaineCalendaire(2026, 19),
          Optional.of(LE_11_MAI_2026_A_9H15)
        );
      assertThat(hours.dureeOperationnelleTotale().valeur()).isEqualTo(Duration.ofHours(9));
      var cost = application
        .getBean(CoutsDeRevientApplicationService.class)
        .rapport(new com.glm.glmback.coutderevient.domain.ElementId(ELEMENT_OF_2026_000042.uuid()));
      assertThat(cost.temps().total().valeur()).isEqualTo(Duration.ofHours(9));
      assertThat(cost.lignes()).hasSize(1);
      var terminal = application.getBean(ReferentielsDuPupitreApplicationService.class).referentiel();
      assertThat(terminal.operateurs()).hasSize(1);
      assertThat(terminal.suivis())
        .singleElement()
        .satisfies(tile -> assertThat(tile.id().uuid()).isEqualTo(followup.uuid()));
      assertThat(atelier.get(followup).suivi().journal().evenements()).hasSize(2);
      return null;
    });
  }

  private static <T> T onTenant(String tenant, Supplier<T> action) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    TenantSecurityContexts.authenticateOn(tenant);
    try {
      return action.get();
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }

  private static Snapshot snapshot(PostgreSQLContainer database, String schema) throws SQLException {
    List<String> catalogue = query(
      database,
      "SELECT table_name || '.' || column_name || ':' || data_type || ':' || is_nullable || ':' || coalesce(column_default, '') "
        + "FROM information_schema.columns WHERE table_schema = '"
        + schema
        + "' ORDER BY table_name, ordinal_position"
    );
    catalogue.addAll(
      query(database, "SELECT indexname || ':' || indexdef FROM pg_indexes WHERE schemaname = '" + schema + "' ORDER BY indexname")
    );
    catalogue.addAll(
      query(
        database,
        "SELECT conname || ':' || pg_get_constraintdef(c.oid) FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace "
          + "WHERE n.nspname = '"
          + schema
          + "' ORDER BY conname"
      )
    );
    List<String> data = new ArrayList<>();
    for (String table : tables(database, schema)) {
      if (!table.startsWith("databasechangelog")) {
        data.addAll(
          query(
            database,
            "SELECT '"
              + table
              + "' || ':' || row_to_json(t)::text FROM \""
              + schema
              + "\".\""
              + table
              + "\" t ORDER BY row_to_json(t)::text"
          )
        );
      }
    }
    return new Snapshot(
      catalogue,
      query(database, "SELECT row_to_json(h)::text FROM \"" + schema + "\".databasechangelog h ORDER BY orderexecuted"),
      data
    );
  }

  private static List<String> tables(PostgreSQLContainer database, String schema) throws SQLException {
    return query(database, "SELECT tablename FROM pg_tables WHERE schemaname = '" + schema + "' ORDER BY tablename");
  }

  private static List<String> query(PostgreSQLContainer database, String sql) throws SQLException {
    try (
      Connection connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
      Statement statement = connection.createStatement();
      var rows = statement.executeQuery(sql)
    ) {
      assertThat(connection.getSchema()).isEqualTo("public");
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

  private record Snapshot(List<String> catalogue, List<String> history, List<String> data) {}
}

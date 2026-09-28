package com.glm.glmback.atelier.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;

@IntegrationTest
class SuppressionDesEvenementsDePauseIT {

  @Autowired
  private DataSource dataSource;

  @Test
  void shouldRetirerLesAnciensPointagesEtRendreLesJourneesRelisibles() throws Exception {
    String schema = "migration_presence_" + UUID.randomUUID().toString().replace("-", "");

    try {
      prepare(schema);
      migre(schema);

      assertThat(types(schema)).containsExactlyInAnyOrder(
        TypeDEvenementDePresence.ARRIVEE,
        TypeDEvenementDePresence.ARRIVEE,
        TypeDEvenementDePresence.DEPART
      );
      assertThat(etat(schema, "00000000-0000-0000-0000-000000000001")).isEqualTo(EtatDePresence.PRESENT);
      assertThat(dernierFait(schema, "00000000-0000-0000-0000-000000000001")).isEqualTo(Instant.parse("2026-05-11T07:00:00Z"));
      assertThat(etat(schema, "00000000-0000-0000-0000-000000000002")).isEqualTo(EtatDePresence.ABSENT);
      assertThat(dernierFait(schema, "00000000-0000-0000-0000-000000000002")).isEqualTo(Instant.parse("2026-05-11T17:00:00Z"));
    } finally {
      execute("drop schema if exists " + schema + " cascade");
    }
  }

  private void prepare(String schema) throws SQLException {
    execute("create schema " + schema);
    execute("create table " + schema + ".journee_de_travail (id uuid primary key, etat varchar(20), dernier_fait timestamptz)");
    execute(
      "create table "
        + schema
        + ".evenement_de_presence "
        + "(id uuid primary key, journee_id uuid, type varchar(20), date_de_survenue timestamptz, annulation_date timestamptz)"
    );
    execute(
      "insert into "
        + schema
        + ".journee_de_travail values "
        + "('00000000-0000-0000-0000-000000000001', 'EN_PAUSE', '2026-05-11T12:00:00Z'), "
        + "('00000000-0000-0000-0000-000000000002', 'ABSENT', '2026-05-11T17:00:00Z')"
    );
    execute(
      "insert into "
        + schema
        + ".evenement_de_presence values "
        + "('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'ARRIVEE', '2026-05-11T07:00:00Z', null), "
        + "('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', 'PAUSE', '2026-05-11T12:00:00Z', null), "
        + "('00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000002', 'ARRIVEE', '2026-05-11T07:00:00Z', null), "
        + "('00000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000002', 'PAUSE', '2026-05-11T12:00:00Z', null), "
        + "('00000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000002', 'REPRISE', '2026-05-11T13:00:00Z', null), "
        + "('00000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000002', 'DEPART', '2026-05-11T17:00:00Z', null), "
        + "('00000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000002', 'PAUSE', '2026-05-11T18:00:00Z', '2026-05-11T19:00:00Z')"
    );
  }

  private void migre(String schema) throws Exception {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setResourceLoader(new DefaultResourceLoader());
    liquibase.setChangeLog("classpath:config/liquibase/changelog/2026/09/005-suppression_evenements_de_pause.xml");
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.afterPropertiesSet();
  }

  private List<TypeDEvenementDePresence> types(String schema) throws SQLException {
    List<TypeDEvenementDePresence> types = new ArrayList<>();

    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      ResultSet lignes = statement.executeQuery("select type from " + schema + ".evenement_de_presence order by id");
      while (lignes.next()) {
        types.add(TypeDEvenementDePresence.valueOf(lignes.getString(1)));
      }
    }

    return types;
  }

  private EtatDePresence etat(String schema, String id) throws SQLException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      ResultSet ligne = statement.executeQuery("select etat from " + schema + ".journee_de_travail where id = '" + id + "'");
      ligne.next();
      return EtatDePresence.valueOf(ligne.getString(1));
    }
  }

  private Instant dernierFait(String schema, String id) throws SQLException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      ResultSet ligne = statement.executeQuery("select dernier_fait from " + schema + ".journee_de_travail where id = '" + id + "'");
      ligne.next();
      return ligne.getTimestamp(1).toInstant();
    }
  }

  private void execute(String sql) throws SQLException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.execute(sql);
    }
  }
}

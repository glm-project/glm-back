package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import static com.glm.glmback.atelier.application.gestionanomalies.ResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.atelier.application.gestionanomalies.ApercusDeResolution;
import com.glm.glmback.atelier.application.gestionanomalies.ConfirmerLesActes;
import com.glm.glmback.atelier.application.gestionanomalies.RecuDActe;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.authentication.infrastructure.primary.TestSecurityConfiguration;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.postgresql.PostgreSQLContainer;

class RecusAuRedemarrageIT {

  @Test
  void shouldRetrouverLeRecuCanoniqueApresRedemarrageSansCleNiApercu() throws Exception {
    try (PostgreSQLContainer database = new PostgreSQLContainer("postgres:18.4").withReuse(false)) {
      database.start();
      RecuDActe recu;
      try (var application = start(database)) {
        assertThat(application.getEnvironment().containsProperty("atelier.resolution.apercus.cle-active")).isFalse();
        recu = dansLEntreprise(() -> {
          var suivis = application.getBean(SuiviDAtelierRepository.class);
          var suivi = application.getBean(TransactionTemplate.class).execute(status -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
          var demande = propositionDAnnulationDeTransition(suivi);
          var proposition = application
            .getBean(ApercusDeResolution.class)
            .apercu(demande.commande(), demande.adresse(), demande.revision(), demande.acte(), CONTEXTE_LEROY_IMPECCMOLD)
            .proposition();
          return application.getBean(ConfirmerLesActes.class).confirmer(suivi.id(), proposition, CONTEXTE_LEROY_IMPECCMOLD).recu();
        });
      }
      assertThat(colonnesDuRecu(database)).contains("proposition").doesNotContain("reference", "preuve");
      try (var application = start(database)) {
        assertThat(application.getEnvironment().containsProperty("atelier.resolution.apercus.cle-active")).isFalse();
        dansLEntreprise(() -> {
          var resultat = application
            .getBean(ConfirmerLesActes.class)
            .verifier(recu.proposition().adresse().suivi(), recu.proposition().commande(), CONTEXTE_LEROY_RENOMME_IMPECCMOLD)
            .orElseThrow();
          assertThat(resultat.recu()).isEqualTo(recu);
          assertThat(resultat.dossier().lecture().suivi().revision().value()).isEqualTo(1);
          assertThat(
            resultat.dossier().lecture().suivi().journal().evenement(recu.proposition().adresse().pointage()).orElseThrow().annulation()
          ).isPresent();
          return null;
        });
      }
    }
  }

  private static ConfigurableApplicationContext start(PostgreSQLContainer database) {
    var application = new SpringApplication(GlmprojectApp.class, TestSecurityConfiguration.class);
    application.setAdditionalProfiles("test");
    return application.run(
      "--server.port=0",
      "--spring.datasource.url=" + database.getJdbcUrl(),
      "--spring.datasource.username=" + database.getUsername(),
      "--spring.datasource.password=" + database.getPassword(),
      "--spring.datasource.driver-class-name=org.postgresql.Driver"
    );
  }

  private static List<String> colonnesDuRecu(PostgreSQLContainer database) throws Exception {
    try (
      var connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
      var statement = connection.createStatement();
      var result = statement.executeQuery(
        "select column_name from information_schema.columns where table_schema = 'impeccmold' and table_name = 'recu_d_acte'"
      )
    ) {
      var colonnes = new ArrayList<String>();
      while (result.next()) {
        colonnes.add(result.getString(1));
      }
      return colonnes;
    }
  }

  private static <T> T dansLEntreprise(Supplier<T> action) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    TenantSecurityContexts.authenticateOn("impeccmold");
    try {
      return action.get();
    } finally {
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }
}

package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import static com.glm.glmback.atelier.gestionconflits.domain.ReferencesHistoriquesFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.*;

import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.cucumber.CucumberSecurityConfiguration;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@IntegrationTest
@SpringBootTest(
  classes = { GlmprojectApp.class, CucumberSecurityConfiguration.class },
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "application.multitenancy.tenants[0].id=historique_resolution", "application.multitenancy.tenants[0].schema=historique_resolution",
  }
)
@AutoConfigureRestTestClient
class ResolutionHistoriqueSansFicheIT {

  @Autowired
  private RestTestClient rest;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("historique_resolution")
  @SuppressWarnings("unchecked")
  void shouldAnnulerUneFinOrphelineAvecSesIdentitesBrutesSansActiviteInventee() {
    var historique = suiviDu10Mai2026SansFicheDOperateur();
    var requetePrecedente = RequestContextHolder.getRequestAttributes();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    try {
      transactions.executeWithoutResult(status -> suivis.create(historique));
    } finally {
      RequestContextHolder.setRequestAttributes(requetePrecedente);
    }
    var fin = historique.journal().evenements().getLast();
    String token = Base64.getEncoder().encodeToString(
      "gestionnaire|ROLE_GESTIONNAIRE|historique_resolution".getBytes(StandardCharsets.UTF_8)
    );
    var http = rest
      .mutate()
      .requestInterceptor((request, body, execution) -> {
        request.getHeaders().setBearerAuth(token);
        return execution.execute(request, body);
      })
      .build();
    String uri = "/api/atelier/suivis/" + historique.id().uuid();
    String dossierUri = uri + "/conflits/" + fin.id().uuid();
    var dossier = get(http, dossierUri);
    assertThat(dossier.get("kind")).isEqualTo("EN_CONFLIT");
    assertThat((List<?>) dossier.get("activites")).isEmpty();
    var sequence = (Map<String, Object>) dossier.get("sequence");
    assertThat(sequence.get("operateurId")).isEqualTo(fin.operateur().uuid().toString());
    assertThat(sequence.get("operateur")).isNull();
    assertThat(sequence.get("posteId")).isNull();
    var diagnostics = (List<Map<String, Object>>) dossier.get("diagnostics");
    assertThat(diagnostics)
      .singleElement()
      .satisfies(diagnostic -> assertThat(diagnostic.get("raison")).isEqualTo("OUVRANT_ANNULE"));
    var journal = (List<Map<String, Object>>) ((Map<String, Object>) dossier.get("suivi")).get("journal");
    assertThat(journal).allSatisfy(fait -> {
      assertThat(fait.get("operateurId")).isEqualTo(fin.operateur().uuid().toString());
      assertThat(fait.get("operateur")).isNull();
    });
    var liste = get(http, "/api/atelier/conflits?element=" + historique.element().id().uuid());
    assertThat(((Number) liste.get("total")).longValue()).isEqualTo(1);
    assertThat((List<Map<String, Object>>) liste.get("lignes"))
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.get("operateurId")).isEqualTo(fin.operateur().uuid().toString());
        assertThat(ligne.get("operateur")).isNull();
      });
    var acte = Map.of("kind", "ANNULATION", "pointage", fin.id().uuid(), "motif", "Fin orpheline historique");
    String commande = UUID.randomUUID().toString();
    var apercu = post(http, dossierUri + "/apercus", Map.of("commande", commande, "revision", dossier.get("revision"), "acte", acte));
    assertThat((List<?>) ((Map<String, Object>) apercu.get("apres")).get("activites")).isEmpty();
    assertThat(get(http, dossierUri).get("revision")).isEqualTo(dossier.get("revision"));
    assertThat(get(http, uri).get("journal")).isEqualTo(journal);
    http
      .post()
      .uri(uri + "/pointages")
      .contentType(APPLICATION_JSON)
      .body(Map.of("id", UUID.randomUUID(), "type", "DEBUT", "intention", "OUVERTURE", "operateur", fin.operateur().uuid()))
      .exchange()
      .expectStatus()
      .isNotFound()
      .expectBody()
      .jsonPath("$.title")
      .isEqualTo("operateur introuvable");
    assertThat(get(http, dossierUri).get("revision")).isEqualTo(dossier.get("revision"));
    assertThat(get(http, uri).get("journal")).isEqualTo(journal);
    var resultat = post(http, uri + "/confirmations-de-resolution", Map.of("commande", commande, "reference", apercu.get("reference")));
    assertThat(resultat.get("kind")).isEqualTo("ENREGISTREE");
    var dossierApres = (Map<String, Object>) resultat.get("dossier");
    assertThat((List<?>) dossierApres.get("activites")).isEmpty();
    assertThat(((Number) dossierApres.get("revision")).longValue()).isEqualTo(((Number) dossier.get("revision")).longValue() + 1);
    var canonique = get(http, uri + "/confirmations-de-resolution/" + commande);
    assertThat(canonique.get("recu")).isEqualTo(resultat.get("recu"));
    assertThat((List<?>) ((Map<String, Object>) canonique.get("dossier")).get("activites")).isEmpty();
    assertThat(((Number) get(http, "/api/atelier/conflits?element=" + historique.element().id().uuid()).get("total")).longValue()).isZero();
    http.get().uri(uri + "/temps-effectif").exchange().expectStatus().isOk().expectBody().json("[]");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> get(RestTestClient http, String uri) {
    return http.get().uri(uri).exchange().expectStatus().isOk().expectBody(Map.class).returnResult().getResponseBody();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> post(RestTestClient http, String uri, Map<String, Object> body) {
    return http
      .post()
      .uri(uri)
      .contentType(APPLICATION_JSON)
      .body(body)
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(Map.class)
      .returnResult()
      .getResponseBody();
  }
}

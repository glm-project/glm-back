package com.glm.glmback.wire.openapi.infrastructure.primary;

import com.glm.glmback.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The only thing allowed to rewrite the committed specification, and it runs only when asked to.
 *
 * <p>
 * This is a task rather than a test, carried by the integration harness because that harness already boots the
 * application: a Maven plugin would demand a running Postgres and Keycloak during the integration-test phase for the
 * same result. Writing into the source tree is a side effect, which is exactly why it lives here and not in the test
 * that asserts.
 * </p>
 *
 * <p>
 * {@code mvn --batch-mode -ntp verify -Dopenapi.regenerate=true} rewrites the file and stands the comparison down for
 * that run — it would otherwise be judging the version being replaced. The rest of the suite still runs, and a bare
 * {@code mvn verify} afterwards is what confirms the result.
 * </p>
 */
@IntegrationTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = OpenApiSpecification.REGENERATION_FLAG, matches = "true")
class OpenApiSpecificationRegenerationIT {

  @Autowired
  private MockMvc rest;

  @Test
  void shouldRewriteTheCommittedSpecification() throws Exception {
    OpenApiSpecification.overwrite(OpenApiSpecification.served(rest));
  }
}

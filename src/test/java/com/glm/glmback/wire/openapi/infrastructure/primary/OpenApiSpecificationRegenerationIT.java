package com.glm.glmback.wire.openapi.infrastructure.primary;

import com.glm.glmback.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = OpenApiSpecification.REGENERATION_FLAG, matches = "true")
class OpenApiSpecificationRegenerationIT {

  @Autowired
  private MockMvc rest;

  @Test
  void shouldOverwriteTheCommittedSpecificationWithTheServedOne() throws Exception {
    OpenApiSpecification.overwrite(OpenApiSpecification.served(rest));
  }
}

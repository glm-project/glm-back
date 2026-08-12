package com.glm.glmback.cucumber;

import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.cucumber.CucumberConfiguration.CucumberRestClientConfiguration;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@ActiveProfiles("test")
@CucumberContextConfiguration
@AutoConfigureRestTestClient
@SpringBootTest(
  classes = { GlmprojectApp.class, CucumberRestClientConfiguration.class, CucumberSecurityConfiguration.class },
  webEnvironment = WebEnvironment.RANDOM_PORT
)
public class CucumberConfiguration {

  private final CucumberRestClient rest;
  private final CucumberClock clock;

  CucumberConfiguration(CucumberRestClient rest, CucumberClock clock) {
    this.rest = rest;
    this.clock = clock;
  }

  @Before
  public void resetTestContext() {
    CucumberRestTestContext.reset();
  }

  @Before
  public void setupRestClient() {
    rest.setupRestClient();
  }

  @Before
  public void releaseClock() {
    clock.reset();
  }

  @TestConfiguration
  static class CucumberRestClientConfiguration {

    @Bean
    CucumberRestClient cucumberRestClient(RestTestClient rest) {
      return new CucumberRestClient(rest);
    }

    /**
     * Surcharge l'horloge du systeme. Le nom de la methode differe volontairement de {@code clock()}, sans quoi le
     * bean reel gagnerait.
     */
    @Bean
    @Primary
    CucumberClock cucumberClock() {
      return new CucumberClock();
    }
  }
}

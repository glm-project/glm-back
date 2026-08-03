package com.glm.glmback.cucumber;

import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.cucumber.CucumberConfiguration.CucumberRestClientConfiguration;
import com.glm.glmback.cucumber.rest.CucumberRestClient;
import com.glm.glmback.cucumber.rest.CucumberRestTestContext;

@ActiveProfiles("test")
@CucumberContextConfiguration
@AutoConfigureRestTestClient
@SpringBootTest(
  classes = { GlmprojectApp.class, CucumberRestClientConfiguration.class },
  webEnvironment = WebEnvironment.RANDOM_PORT
)
public class CucumberConfiguration {

  private final CucumberRestClient rest;

  CucumberConfiguration(CucumberRestClient rest) {
    this.rest = rest;
  }

  @Before
  public void resetTestContext() {
    CucumberRestTestContext.reset();
  }

  @Before
  public void setupRestClient() {
    rest.setupRestClient();
  }

  @TestConfiguration
  static class CucumberRestClientConfiguration {

    @Bean
    CucumberRestClient cucumberRestClient(RestTestClient rest) {
      return new CucumberRestClient(rest);
    }
  }
}

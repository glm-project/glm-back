package com.glm.glmback.cucumber;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import io.cucumber.java.en.Given;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class CucumberAuthenticationSteps {

  @Autowired
  private CucumberRestClient rest;

  @Given("I am logged in as {string} with role {string}")
  public void iAmLoggedInAsWithRole(String username, String role) {
    String token = Base64.getEncoder().encodeToString(("%s|ROLE_%s".formatted(username, role)).getBytes(StandardCharsets.UTF_8));

    rest.addRequestInterceptor((request, body, execution) -> {
      request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

      return execution.execute(request, body);
    });
  }
}

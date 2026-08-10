package com.glm.glmback.cucumber;

import com.glm.glmback.cucumber.rest.CucumberRestClient;
import io.cucumber.java.en.Given;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class CucumberAuthenticationSteps {

  private static final String TENANT_PAR_DEFAUT = "impeccmold";

  @Autowired
  private CucumberRestClient rest;

  @Given("I am logged in as {string} with role {string}")
  public void iAmLoggedInAsWithRole(String username, String role) {
    authenticate(username, role, TENANT_PAR_DEFAUT);
  }

  @Given("I am logged in as {string} with role {string} for tenant {string}")
  public void iAmLoggedInAsWithRoleForTenant(String username, String role, String tenant) {
    authenticate(username, role, tenant);
  }

  @Given("I am logged in as {string} with role {string} without tenant")
  public void iAmLoggedInAsWithRoleWithoutTenant(String username, String role) {
    authenticate(username, role, "");
  }

  private void authenticate(String username, String role, String tenant) {
    String token = Base64.getEncoder().encodeToString(("%s|ROLE_%s|%s".formatted(username, role, tenant)).getBytes(StandardCharsets.UTF_8));

    rest.addRequestInterceptor((request, body, execution) -> {
      request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

      return execution.execute(request, body);
    });
  }
}

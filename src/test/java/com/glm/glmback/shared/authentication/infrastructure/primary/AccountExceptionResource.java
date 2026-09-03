package com.glm.glmback.shared.authentication.infrastructure.primary;

import com.glm.glmback.shared.authentication.application.NotAuthenticatedUserException;
import com.glm.glmback.shared.authentication.application.UnknownAuthenticationException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hidden from the description : this controller exists only to make the advices throw, and the committed
 * documentation/openapi.json is a deliverable read by the front — a route it could never call has no place in it.
 */
@Hidden
@RestController
@RequestMapping("/api/account-exceptions")
class AccountExceptionResource {

  @GetMapping("/not-authenticated")
  public void notAuthenticatedUser() {
    throw new NotAuthenticatedUserException();
  }

  @GetMapping("/unknown-authentication")
  public void unknownAuthentication() {
    throw new UnknownAuthenticationException();
  }
}

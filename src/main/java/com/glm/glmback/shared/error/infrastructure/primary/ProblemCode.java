package com.glm.glmback.shared.error.infrastructure.primary;

import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public interface ProblemCode {
  String name();

  String context();

  HttpStatus status();

  String title();

  default ProblemDetail problem(RuntimeException e) {
    var problem = ProblemDetail.forStatus(status());
    problem.setType(type());
    problem.setTitle(title());
    problem.setProperty("message", e.getMessage());

    return problem;
  }

  private URI type() {
    return URI.create("urn:glm:erreur:%s:%s".formatted(context(), code()));
  }

  private String code() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}

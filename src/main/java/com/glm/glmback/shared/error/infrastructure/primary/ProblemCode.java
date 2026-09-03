package com.glm.glmback.shared.error.infrastructure.primary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public interface ProblemCode {
  HttpStatus status();

  String title();

  default ProblemDetail problem(RuntimeException e) {
    var problem = ProblemDetail.forStatus(status());
    problem.setTitle(title());
    problem.setProperty("message", e.getMessage());

    return problem;
  }
}

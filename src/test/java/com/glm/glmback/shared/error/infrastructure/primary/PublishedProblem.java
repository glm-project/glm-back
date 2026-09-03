package com.glm.glmback.shared.error.infrastructure.primary;

import org.springframework.http.HttpStatus;

public record PublishedProblem(RuntimeException exception, String type, HttpStatus status) {}

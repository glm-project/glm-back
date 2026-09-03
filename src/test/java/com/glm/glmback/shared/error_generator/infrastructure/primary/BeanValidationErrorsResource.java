package com.glm.glmback.shared.error_generator.infrastructure.primary;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hidden from the description : this controller exists only to make bean validation fail, and the committed
 * documentation/openapi.json is a deliverable read by the front — a route it could never call has no place in it.
 */
@Hidden
@Validated
@RestController
@RequestMapping("/api/bean-validation-errors")
class BeanValidationErrorsResource {

  @PostMapping("mandatory-body-parameter")
  void createMandatoryParameter(@RequestBody @Validated RestMandatoryParameter parameter) {
    // empty method
  }

  @GetMapping("complicated-path-variable/{complicated}")
  void complicatedPathVariable(@PathVariable("complicated") @Pattern(regexp = "complicated") String complicated) {
    // empty method
  }
}

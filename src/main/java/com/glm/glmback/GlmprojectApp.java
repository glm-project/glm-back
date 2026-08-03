package com.glm.glmback;

import com.glm.glmback.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
@ExcludeFromGeneratedCodeCoverage(reason = "Not testing logs")
public class GlmprojectApp {

  private static final Logger log = LoggerFactory.getLogger(GlmprojectApp.class);

  public static void main(String[] args) {
    Environment env = SpringApplication.run(GlmprojectApp.class, args).getEnvironment();

    if (log.isInfoEnabled()) {
      log.info(ApplicationStartupTraces.of(env));
    }
  }
}

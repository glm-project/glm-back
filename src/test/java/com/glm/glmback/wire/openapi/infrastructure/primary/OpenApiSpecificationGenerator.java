package com.glm.glmback.wire.openapi.infrastructure.primary;

import com.glm.glmback.GlmprojectApp;
import com.glm.glmback.shared.authentication.infrastructure.primary.TestSecurityConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Not a test: a plain {@code main()}, run by exec-maven-plugin on every {@code mvn verify} (see pom.xml), that
 * regenerates {@code documentation/openapi.json}. Boots the real application exactly like {@code IntegrationTest}
 * does (GlmprojectApp + TestSecurityConfiguration, test profile), then hits the real embedded server over HTTP —
 * no MockMvc, no JUnit.
 */
public final class OpenApiSpecificationGenerator {

  private static final Path FILE = Path.of("documentation", "openapi.json");
  private static final String SERVER_URL_SPRINGDOC_DERIVES_FROM_THE_REQUEST = "servers";
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};
  private static final ObjectMapper SORTED_KEYS_AND_FIXED_INDENTATION = JsonMapper.builder()
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .build();

  private OpenApiSpecificationGenerator() {}

  public static void main(String[] args) throws Exception {
    try (
      ConfigurableApplicationContext context = new SpringApplicationBuilder(GlmprojectApp.class, TestSecurityConfiguration.class)
        .profiles("test")
        .run(args)
    ) {
      int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
      Files.writeString(FILE, canonical(served(port)) + "\n", StandardCharsets.UTF_8);
    }

    System.exit(0);
  }

  private static String served(int port) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
      .timeout(HTTP_TIMEOUT)
      .GET()
      .build();
    HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    if (response.statusCode() != 200) {
      throw new IllegalStateException("GET /v3/api-docs returned " + response.statusCode() + ": " + response.body());
    }
    return response.body();
  }

  private static String canonical(String specification) {
    Map<String, Object> description = SORTED_KEYS_AND_FIXED_INDENTATION.readValue(specification, JSON_OBJECT);
    description.remove(SERVER_URL_SPRINGDOC_DERIVES_FROM_THE_REQUEST);

    return SORTED_KEYS_AND_FIXED_INDENTATION.writeValueAsString(description);
  }
}

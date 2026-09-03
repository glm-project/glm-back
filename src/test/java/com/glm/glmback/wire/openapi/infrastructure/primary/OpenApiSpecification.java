package com.glm.glmback.wire.openapi.infrastructure.primary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The two sides of the specification — the one the application serves and the one committed for the front — in the
 * single form they can be compared in.
 *
 * <p>
 * Both go through the same canonicalisation, so only a change of contract can tell them apart: property order is
 * imposed, indentation is fixed, and {@code servers} is dropped because springdoc derives it from the request
 * ({@code http://localhost} under MockMvc) — a value that would make the committed file tremble for nothing.
 * {@code openapi-typescript} ignores it anyway.
 * </p>
 *
 * <p>
 * The committed side is canonicalised too, rather than compared byte for byte: a re-indentation or a hand edit that
 * moves nothing but whitespace is not a contract break, and reporting it as one would teach the team to regenerate on
 * noise. The file's layout belongs to whatever writes it — which is why {@code .prettierignore} keeps Prettier off it.
 * </p>
 *
 * <p>
 * The file is resolved against the working directory, which Failsafe sets to the project root. A run started from
 * somewhere else — an IDE configuration, typically — will not find it, so the failure prints the path it actually
 * looked at rather than the relative one.
 * </p>
 */
final class OpenApiSpecification {

  static final String REGENERATION_FLAG = "openapi.regenerate";

  private static final Path FILE = Path.of("documentation", "openapi.json");
  private static final String REGENERATION_COMMAND = "mvn --batch-mode -ntp verify -D" + REGENERATION_FLAG + "=true";
  private static final String SERVERS = "servers";
  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};
  private static final ObjectMapper CANONICAL = JsonMapper.builder()
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .build();

  private OpenApiSpecification() {}

  static String served(MockMvc rest) throws Exception {
    return canonical(rest.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  static String committed() {
    try {
      return canonical(Files.readString(FILE, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + FILE.toAbsolutePath() + ". " + regenerationHint(), e);
    }
  }

  static void overwrite(String specification) {
    try {
      Files.writeString(FILE, specification + "\n", StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write " + FILE.toAbsolutePath(), e);
    }
  }

  static String outOfDateMessage() {
    return (
      "The served specification no longer matches "
      + FILE
      + ". "
      + regenerationHint()
      + " Then read `git diff "
      + FILE
      + "`: that diff, not the dump below, is what tells you which route or schema moved."
    );
  }

  private static String regenerationHint() {
    return "Regenerate it with: " + REGENERATION_COMMAND + ".";
  }

  private static String canonical(String specification) {
    Map<String, Object> description = CANONICAL.readValue(specification, JSON_OBJECT);
    description.remove(SERVERS);

    return CANONICAL.writeValueAsString(description);
  }
}

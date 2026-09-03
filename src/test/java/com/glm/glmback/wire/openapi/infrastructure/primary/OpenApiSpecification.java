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

final class OpenApiSpecification {

  static final String REGENERATION_FLAG = "openapi.regenerate";

  private static final Path FILE = Path.of("documentation", "openapi.json");
  private static final String REGENERATION_COMMAND = "mvn --batch-mode -ntp verify -D" + REGENERATION_FLAG + "=true";
  private static final String SERVER_URL_SPRINGDOC_DERIVES_FROM_THE_REQUEST = "servers";
  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};
  private static final ObjectMapper SORTED_KEYS_AND_FIXED_INDENTATION = JsonMapper.builder()
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
    return "The served specification no longer matches " + FILE + ". " + regenerationHint() + " Then read `git diff " + FILE + "`.";
  }

  private static String regenerationHint() {
    return "Regenerate it with: " + REGENERATION_COMMAND + ".";
  }

  private static String canonical(String specification) {
    Map<String, Object> description = SORTED_KEYS_AND_FIXED_INDENTATION.readValue(specification, JSON_OBJECT);
    description.remove(SERVER_URL_SPRINGDOC_DERIVES_FROM_THE_REQUEST);

    return SORTED_KEYS_AND_FIXED_INDENTATION.writeValueAsString(description);
  }
}

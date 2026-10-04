package com.glm.glmback.atelier.gestionconflits.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class ConfigurationDesApercusTest {

  private static final String CLE = Base64.getEncoder().encodeToString(new byte[32]);

  @ParameterizedTest
  @MethodSource("configurationsInvalides")
  void shouldRefuserUnDeploiementSansCleExpliciteValide(ConfigurationInvalid candidate) {
    assertThatThrownBy(() -> new ConfigurationDesApercus(candidate.active(), candidate.cles(), candidate.validite())).isInstanceOf(
      IllegalArgumentException.class
    );
  }

  @Test
  void shouldConserverUneCopieDuTrousseauPartage() {
    var cles = new HashMap<>(Map.of("active", CLE));
    var configuration = new ConfigurationDesApercus("active", cles, Duration.ofMinutes(15));
    cles.clear();
    assertThat(configuration.cles()).containsEntry("active", CLE);
    assertThatThrownBy(() -> configuration.cles().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  private static Stream<ConfigurationInvalid> configurationsInvalides() {
    return Stream.of(
      new ConfigurationInvalid(null, Map.of("active", CLE), Duration.ofMinutes(15)),
      new ConfigurationInvalid("", Map.of("active", CLE), Duration.ofMinutes(15)),
      new ConfigurationInvalid("bad.key", Map.of("bad.key", CLE), Duration.ofMinutes(15)),
      new ConfigurationInvalid("active", null, Duration.ofMinutes(15)),
      new ConfigurationInvalid("active", Map.of(), Duration.ofMinutes(15)),
      new ConfigurationInvalid("active", Map.of("old", CLE), Duration.ofMinutes(15)),
      new ConfigurationInvalid("active", Map.of("active", "%%%"), Duration.ofMinutes(15)),
      new ConfigurationInvalid("active", Map.of("active", Base64.getEncoder().encodeToString(new byte[16])), Duration.ofMinutes(15)),
      new ConfigurationInvalid("active", Map.of("active", CLE, "bad.key", CLE), Duration.ofMinutes(15)),
      new ConfigurationInvalid("active", Map.of("active", CLE), null),
      new ConfigurationInvalid("active", Map.of("active", CLE), Duration.ZERO),
      new ConfigurationInvalid("active", Map.of("active", CLE), Duration.ofSeconds(-1))
    );
  }

  private record ConfigurationInvalid(String active, Map<String, String> cles, Duration validite) {}
}

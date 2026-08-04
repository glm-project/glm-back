package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class InMemoryPrefixesDElementsDeFabricationTest {

  private final InMemoryPrefixesDElementsDeFabrication prefixes = new InMemoryPrefixesDElementsDeFabrication();

  @Test
  void shouldGetPrefixeDOrdreDeFabrication() {
    assertThat(prefixes.prefixeDOrdreDeFabrication()).isEqualTo(PREFIXE_OF);
  }

  @Test
  void shouldGetPrefixeDeProduit() {
    assertThat(prefixes.prefixeDeProduit()).isEqualTo(PREFIXE_PRD);
  }
}

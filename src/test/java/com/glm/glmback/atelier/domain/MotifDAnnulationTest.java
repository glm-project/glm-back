package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class MotifDAnnulationTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new MotifDAnnulation(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("motif d'annulation");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new MotifDAnnulation(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("motif d'annulation");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(256);

    assertThatThrownBy(() -> new MotifDAnnulation(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("motif d'annulation");
  }

  @Test
  void shouldGetValueFromValidMotifDAnnulation() {
    assertThat(MOTIF_ERREUR_DE_SAISIE.value()).isEqualTo("Erreur de saisie");
  }
}

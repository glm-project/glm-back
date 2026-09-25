package com.glm.glmback.parametrage.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class AmplitudeMaximaleTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new AmplitudeMaximale(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("amplitude maximale");
  }

  @ParameterizedTest
  @ValueSource(strings = { "PT0S", "PT-1H", "PT0.5S", "PT59S" })
  void shouldNotBuildWithoutAtLeastOneMinute(String valeur) {
    Duration duree = Duration.parse(valeur);

    assertThatThrownBy(() -> new AmplitudeMaximale(duree))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("amplitude maximale");
  }

  /**
   * Le seuil reste sous 24 h : c'est ce qui garantit qu'un retour le lendemain a la meme heure soit toujours une
   * nouvelle arrivee.
   */
  @ParameterizedTest
  @ValueSource(strings = { "PT24H", "PT25H", "P365D" })
  void shouldNotBuildFromTwentyFourHours(String valeur) {
    Duration duree = Duration.parse(valeur);

    assertThatThrownBy(() -> new AmplitudeMaximale(duree))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("amplitude maximale");
  }

  @ParameterizedTest
  @ValueSource(strings = { "PT10H0M30S", "PT12H30M0.001S", "PT23H59M59S" })
  void shouldNotBuildWithSeconds(String valeur) {
    Duration duree = Duration.parse(valeur);

    assertThatThrownBy(() -> new AmplitudeMaximale(duree))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("amplitude maximale");
  }

  @ParameterizedTest
  @ValueSource(strings = { "PT1M", "PT10H", "PT12H30M", "PT13H", "PT23H59M" })
  void shouldBuildWithWholeMinutesStrictlyBetweenZeroAndTwentyFourHours(String valeur) {
    Duration duree = Duration.parse(valeur);

    assertThat(new AmplitudeMaximale(duree).value()).isEqualTo(duree);
  }
}

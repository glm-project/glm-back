package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class TempsPasseTest {

  @Test
  void shouldNotBuildWithoutTravail() {
    assertThatThrownBy(() -> new TempsPasse(null, Duration.ZERO))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("temps de travail");
  }

  @Test
  void shouldNotBuildWithoutNonConformite() {
    assertThatThrownBy(() -> new TempsPasse(Duration.ZERO, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("temps en non conformite");
  }

  @Test
  void shouldAddBothCategories() {
    TempsPasse temps = new TempsPasse(Duration.ofHours(2), Duration.ofMinutes(30));

    assertThat(temps.total()).isEqualTo(Duration.ofMinutes(150));
  }

  @Test
  void shouldSumTempsPasse() {
    TempsPasse somme = new TempsPasse(Duration.ofHours(2), Duration.ofMinutes(30)).plus(
      new TempsPasse(Duration.ofHours(1), Duration.ofMinutes(15))
    );

    assertThat(somme).isEqualTo(new TempsPasse(Duration.ofHours(3), Duration.ofMinutes(45)));
  }

  @Test
  void shouldStartFromAucun() {
    assertThat(TempsPasse.AUCUN.total()).isZero();
  }
}

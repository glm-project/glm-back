package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class AnneeTest {

  @Test
  void shouldNotBuildWithAnneeBeforeFirstOne() {
    assertThatThrownBy(() -> new Annee(1999))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("annee");
  }

  @Test
  void shouldNotBuildWithAnneeAfterLastOne() {
    assertThatThrownBy(() -> new Annee(10000))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("annee");
  }

  @Test
  void shouldGetValueFromValidAnnee() {
    assertThat(ANNEE_2026.value()).isEqualTo(2026);
  }

  @Test
  void shouldReadAnneeFromInstant() {
    assertThat(Annee.de(LE_15_JANVIER_2026)).isEqualTo(ANNEE_2026);
  }

  @Test
  void shouldReadAnneeFromInstantOnLastSecondOfYear() {
    assertThat(Annee.de(Instant.parse("2026-12-31T23:59:59Z"))).isEqualTo(ANNEE_2026);
  }
}

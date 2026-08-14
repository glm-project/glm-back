package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PlageDUnJourTest {

  private static final LocalDate LUNDI_11_MAI_2026 = LocalDate.of(2026, 5, 11);
  private static final Plage DE_8H_A_12H = new Plage(
    Instant.parse("2026-05-11T06:00:00Z"),
    Optional.of(Instant.parse("2026-05-11T10:00:00Z"))
  );

  @Test
  void shouldNotBuildWithoutJour() {
    assertThatThrownBy(() -> new PlageDUnJour(null, DE_8H_A_12H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jour");
  }

  @Test
  void shouldNotBuildWithoutPlage() {
    assertThatThrownBy(() -> new PlageDUnJour(LUNDI_11_MAI_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("plage");
  }

  @Test
  void shouldPorterSonJourEtSaPlage() {
    PlageDUnJour matinee = new PlageDUnJour(LUNDI_11_MAI_2026, DE_8H_A_12H);

    assertThat(matinee.jour()).isEqualTo(LUNDI_11_MAI_2026);
    assertThat(matinee.plage()).isEqualTo(DE_8H_A_12H);
  }
}

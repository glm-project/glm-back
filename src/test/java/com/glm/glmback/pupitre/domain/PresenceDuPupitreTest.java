package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PresenceDuPupitreTest {

  @Test
  void shouldNotBuildWithoutEtat() {
    assertThatThrownBy(() -> new PresenceDuPupitre(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("etat de presence");
  }

  @Test
  void shouldNotBuildWithoutPresentJusqua() {
    assertThatThrownBy(() -> new PresenceDuPupitre(EtatDePresence.ABSENT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("present jusqu'a");
  }

  @Test
  void shouldEtreAbsentSansEcheance() {
    assertThat(PresenceDuPupitre.absente()).isEqualTo(new PresenceDuPupitre(EtatDePresence.ABSENT, Optional.empty()));
  }
}

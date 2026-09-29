package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class JourDeSyntheseTest {

  @Test
  void shouldNotBuildWithoutJour() {
    assertThatThrownBy(() -> new JourDeSynthese(null, List.of(), Duration.ZERO)).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldNotBuildWithoutPointages() {
    assertThatThrownBy(() -> new JourDeSynthese(LUNDI_11_MAI_2026, null, Duration.ZERO)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
  }

  @Test
  void shouldNotBuildWithNullPointage() {
    assertThatThrownBy(() ->
      new JourDeSynthese(LUNDI_11_MAI_2026, Arrays.asList((PointageDElement) null), Duration.ZERO)
    ).isExactlyInstanceOf(NullElementInCollectionException.class);
  }

  @Test
  void shouldNotBuildWithoutDureeOperationnelle() {
    assertThatThrownBy(() -> new JourDeSynthese(LUNDI_11_MAI_2026, List.of(), null)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
  }
}

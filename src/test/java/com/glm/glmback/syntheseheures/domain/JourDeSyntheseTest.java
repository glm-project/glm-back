package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class JourDeSyntheseTest {

  private static final LocalDate LUNDI_11_MAI_2026 = LocalDate.of(2026, 5, 11);
  private static final EvenementDePresence ARRIVEE = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);

  @Test
  void shouldNotBuildWithoutJour() {
    assertThatThrownBy(() -> new JourDeSynthese(null, List.of(), Duration.ZERO))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jour");
  }

  @Test
  void shouldNotBuildWithoutPointages() {
    assertThatThrownBy(() -> new JourDeSynthese(LUNDI_11_MAI_2026, null, Duration.ZERO))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("pointages");
  }

  @Test
  void shouldNotBuildWithNullPointage() {
    List<EvenementDePresence> pointages = Arrays.asList(ARRIVEE, null);

    assertThatThrownBy(() -> new JourDeSynthese(LUNDI_11_MAI_2026, pointages, Duration.ZERO))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("pointages");
  }

  @Test
  void shouldNotBuildWithoutDuree() {
    assertThatThrownBy(() -> new JourDeSynthese(LUNDI_11_MAI_2026, List.of(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree");
  }

  @Test
  void shouldPorterSonJourSesPointagesEtSaDuree() {
    JourDeSynthese jour = new JourDeSynthese(LUNDI_11_MAI_2026, List.of(ARRIVEE), Duration.ofHours(8));

    assertThat(jour.jour()).isEqualTo(LUNDI_11_MAI_2026);
    assertThat(jour.pointages()).containsExactly(ARRIVEE);
    assertThat(jour.duree()).isEqualTo(Duration.ofHours(8));
  }
}

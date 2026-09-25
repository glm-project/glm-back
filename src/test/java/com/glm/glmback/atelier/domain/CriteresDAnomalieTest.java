package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CriteresDAnomalieTest {

  private static final JourneeDeTravail SANS_DEPART = journeeDeDupontOuverteA7H();
  private static final JourneeDeTravail SEIZE_HEURES = journeeDeDupontOuverteA7H().enregistre(
    departDeDupontA(LE_10_MAI_2026_A_7H.plus(Duration.ofHours(16)))
  );

  @Test
  void shouldNotBuildWithoutMaintenant() {
    assertThatThrownBy(() -> new CriteresDAnomalie(null, AMPLITUDE_MAXIMALE_13H, Optional.empty(), Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("maintenant");
  }

  @Test
  void shouldNotBuildWithoutSeuil() {
    assertThatThrownBy(() -> new CriteresDAnomalie(LE_11_MAI_2026_A_9H, null, Optional.empty(), Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("seuil");
  }

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new CriteresDAnomalie(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new CriteresDAnomalie(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldRetenirToutesLesAnomaliesSansFiltre() {
    CriteresDAnomalie criteres = new CriteresDAnomalie(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.empty(), Optional.empty());

    assertThat(criteres.matches(SANS_DEPART)).isTrue();
    assertThat(criteres.matches(SEIZE_HEURES)).isTrue();
    assertThat(criteres.matches(journeeDeDupontDe7HA17HAvecPauseDeMidi())).isFalse();
  }

  @Test
  void shouldFiltrerParType() {
    CriteresDAnomalie sansDepart = new CriteresDAnomalie(
      LE_11_MAI_2026_A_9H,
      AMPLITUDE_MAXIMALE_13H,
      Optional.empty(),
      Optional.of(TypeDAnomalie.JOURNEE_SANS_DEPART)
    );

    assertThat(sansDepart.matches(SANS_DEPART)).isTrue();
    assertThat(sansDepart.matches(SEIZE_HEURES)).isFalse();
  }

  @Test
  void shouldFiltrerParOperateur() {
    CriteresDAnomalie deMartin = new CriteresDAnomalie(
      LE_11_MAI_2026_A_9H,
      AMPLITUDE_MAXIMALE_13H,
      Optional.of(OPERATEUR_ID_MARTIN),
      Optional.empty()
    );
    CriteresDAnomalie deDupont = new CriteresDAnomalie(
      LE_11_MAI_2026_A_9H,
      AMPLITUDE_MAXIMALE_13H,
      Optional.of(OPERATEUR_ID_DUPONT),
      Optional.empty()
    );

    assertThat(deMartin.matches(SANS_DEPART)).isFalse();
    assertThat(deDupont.matches(SANS_DEPART)).isTrue();
  }
}

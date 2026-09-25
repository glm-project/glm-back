package com.glm.glmback.parametrage.domain;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ParametrageTest {

  @Test
  void shouldNotBuildWithoutAmplitudeMaximale() {
    assertThatThrownBy(() -> new Parametrage(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("amplitude maximale");
  }

  @Test
  void shouldNotBuildWithoutDerniereModification() {
    assertThatThrownBy(() -> new Parametrage(AMPLITUDE_MAXIMALE_13H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("derniere modification");
  }

  @Test
  void shouldFixerLAmplitudeMaximaleDUnParametrageJamaisModifie() {
    Parametrage fixe = parametrageParDefaut().fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_10H, modificationParLeroyLe24Septembre());

    assertThat(fixe.amplitudeMaximale()).isEqualTo(AMPLITUDE_MAXIMALE_10H);
    assertThat(fixe.derniereModification()).contains(modificationParLeroyLe24Septembre());
  }

  @Test
  void shouldGarderLaTraceDeLaSeuleDerniereModification() {
    Parametrage fixe = parametrageA10HParLeroyLe24Septembre().fixeLAmplitudeMaximale(
      AMPLITUDE_MAXIMALE_12H30,
      modificationParMartinLe25Septembre()
    );

    assertThat(fixe.amplitudeMaximale()).isEqualTo(AMPLITUDE_MAXIMALE_12H30);
    assertThat(fixe.derniereModification()).contains(modificationParMartinLe25Septembre());
  }

  @Test
  void shouldTracerUneValeurRefixeeALIdentique() {
    Parametrage fixe = parametrageA10HParLeroyLe24Septembre().fixeLAmplitudeMaximale(
      AMPLITUDE_MAXIMALE_10H,
      modificationParMartinLe25Septembre()
    );

    assertThat(fixe.derniereModification()).contains(modificationParMartinLe25Septembre());
  }

  @Test
  void shouldAccepterUneModificationAuMemeInstantQueLaPrecedente() {
    Modification simultanee = new Modification(AUTEUR_MARTIN, LE_24_SEPTEMBRE_2026_A_9H);

    Parametrage fixe = parametrageA10HParLeroyLe24Septembre().fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_13H, simultanee);

    assertThat(fixe.derniereModification()).contains(simultanee);
  }

  /**
   * L'histoire ne se reecrit pas : une modification ne peut preceder celle qu'elle remplace.
   */
  @Test
  void shouldRefuserUneModificationAnterieureALaPrecedente() {
    Parametrage parametrage = parametrageA10HParLeroyLe24Septembre();
    Modification anterieure = new Modification(AUTEUR_MARTIN, Instant.parse("2026-09-23T09:00:00Z"));

    assertThatThrownBy(() -> parametrage.fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_13H, anterieure))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("date de modification");
  }

  @Test
  void shouldNotFixerSansAmplitude() {
    Parametrage parametrage = parametrageParDefaut();
    Modification modification = modificationParLeroyLe24Septembre();

    assertThatThrownBy(() -> parametrage.fixeLAmplitudeMaximale(null, modification))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("amplitude maximale");
  }

  @Test
  void shouldNotFixerSansModification() {
    Parametrage parametrage = parametrageParDefaut();

    assertThatThrownBy(() -> parametrage.fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_10H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("modification");
  }
}

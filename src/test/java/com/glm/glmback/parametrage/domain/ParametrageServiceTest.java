package com.glm.glmback.parametrage.domain;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ParametrageServiceTest {

  @Test
  void shouldLireLeParametrageSemeDUneEntrepriseNeuve() {
    ParametrageService service = new ParametrageService(ParametrageEnMemoire.seme(parametrageParDefaut()), () -> LE_24_SEPTEMBRE_2026_A_9H);

    Parametrage parametrage = service.get();

    assertThat(parametrage.amplitudeMaximale()).isEqualTo(AMPLITUDE_MAXIMALE_13H);
    assertThat(parametrage.derniereModification()).isEmpty();
  }

  @Test
  void shouldFixerLAmplitudeMaximaleALHeureDeLHorloge() {
    ParametrageEnMemoire repository = ParametrageEnMemoire.seme(parametrageParDefaut());
    ParametrageService service = new ParametrageService(repository, () -> LE_24_SEPTEMBRE_2026_A_9H);

    Parametrage fixe = service.fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_10H, AUTEUR_LEROY);

    assertThat(fixe).isEqualTo(parametrageA10HParLeroyLe24Septembre());
    assertThat(repository.get()).isEqualTo(parametrageA10HParLeroyLe24Septembre());
  }

  @Test
  void shouldGarderLaDerniereDeDeuxModificationsSuccessives() {
    ParametrageEnMemoire repository = ParametrageEnMemoire.seme(parametrageA10HParLeroyLe24Septembre());
    ParametrageService service = new ParametrageService(repository, () -> LE_25_SEPTEMBRE_2026_A_9H);

    service.fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_12H30, AUTEUR_MARTIN);

    assertThat(service.get().amplitudeMaximale()).isEqualTo(AMPLITUDE_MAXIMALE_12H30);
    assertThat(service.get().derniereModification()).contains(modificationParMartinLe25Septembre());
  }

  @Test
  void shouldTracerUneValeurRefixeeALIdentique() {
    ParametrageEnMemoire repository = ParametrageEnMemoire.seme(parametrageA10HParLeroyLe24Septembre());
    ParametrageService service = new ParametrageService(repository, () -> LE_25_SEPTEMBRE_2026_A_9H);

    service.fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_10H, AUTEUR_MARTIN);

    assertThat(repository.get().derniereModification()).contains(modificationParMartinLe25Septembre());
  }

  @Test
  void shouldSignalerUnParametrageAbsent() {
    ParametrageService service = new ParametrageService(ParametrageEnMemoire.vide(), () -> LE_24_SEPTEMBRE_2026_A_9H);

    assertThatThrownBy(service::get).isExactlyInstanceOf(ParametrageIntrouvableException.class).hasMessageContaining("parametrage");
  }

  @Test
  void shouldNeRienFixerSurUnParametrageAbsent() {
    ParametrageService service = new ParametrageService(ParametrageEnMemoire.vide(), () -> LE_24_SEPTEMBRE_2026_A_9H);

    assertThatThrownBy(() -> service.fixeLAmplitudeMaximale(AMPLITUDE_MAXIMALE_10H, AUTEUR_LEROY)).isExactlyInstanceOf(
      ParametrageIntrouvableException.class
    );
  }
}

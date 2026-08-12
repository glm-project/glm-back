package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class FenetreDePresenceTest {

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new FenetreDePresence(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> new FenetreDePresence(LE_10_MAI_2026_A_8H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new FenetreDePresence(LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_8H)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldBeOuverteWithoutFin() {
    assertThat(new FenetreDePresence(LE_10_MAI_2026_A_8H, Optional.empty()).estOuverte()).isTrue();
  }

  @Test
  void shouldNotBeOuverteWithFin() {
    assertThat(matinee().estOuverte()).isFalse();
  }

  @Test
  void shouldGarderLesBornesLesPlusResserrees() {
    FenetreDePresence part = matinee().intersection(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_9H)).orElseThrow();

    assertThat(part.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(part.fin()).contains(LE_10_MAI_2026_A_9H);
  }

  @Test
  void shouldGarderLesBornesDeLAutreIntervalleQuandIlEstLePlusEtroit() {
    FenetreDePresence part = matinee().intersection(LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_9H.plusSeconds(60))).orElseThrow();

    assertThat(part.debut()).isEqualTo(LE_10_MAI_2026_A_9H);
    assertThat(part.fin()).contains(LE_10_MAI_2026_A_9H.plusSeconds(60));
  }

  @Test
  void shouldFermerUnIntervalleOuvertSurLaFinDeLaFenetre() {
    FenetreDePresence part = matinee().intersection(LE_10_MAI_2026_A_9H, Optional.empty()).orElseThrow();

    assertThat(part.fin()).contains(LE_10_MAI_2026_A_12H);
  }

  @Test
  void shouldLaisserOuvertUnIntervalleOuvertDansUneFenetreOuverte() {
    FenetreDePresence apresMidi = new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.empty());

    FenetreDePresence part = apresMidi.intersection(LE_10_MAI_2026_A_17H, Optional.empty()).orElseThrow();

    assertThat(part.debut()).isEqualTo(LE_10_MAI_2026_A_17H);
    assertThat(part.fin()).isEmpty();
  }

  @Test
  void shouldNotIntersecterUnIntervalleDisjoint() {
    assertThat(matinee().intersection(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))).isEmpty();
  }

  @Test
  void shouldNotIntersecterUnIntervalleReduitAUnInstant() {
    assertThat(matinee().intersection(LE_10_MAI_2026_A_12H, Optional.empty())).isEmpty();
  }

  private static FenetreDePresence matinee() {
    return new FenetreDePresence(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H));
  }
}

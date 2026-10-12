package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PassageEnAtelierTest {

  @Test
  void shouldConstruireUnPassageOuvert() {
    var passage = new PassageOuvert(
      new SuiviId(UUID.fromString("5f0c6d1e-2a3b-4c5d-8e9f-0a1b2c3d4e5f")),
      Instant.parse("2026-05-10T07:00:00Z"),
      EtatEnAtelier.EN_ATTENTE
    );

    assertThat(passage.suivi()).isEqualTo(SUIVI_DU_10_MAI_2026);
    assertThat(passage.misALAtelierLe()).isEqualTo(INSTANT_10_MAI_2026_A_7H);
    assertThat(passage.etat()).isEqualTo(EtatEnAtelier.EN_ATTENTE);
  }

  @Test
  void shouldNotBuildUnPassageOuvertSansSuivi() {
    assertThatThrownBy(() -> new PassageOuvert(null, INSTANT_10_MAI_2026_A_7H, EtatEnAtelier.EN_COURS))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("suivi");
  }

  @Test
  void shouldNotBuildUnPassageOuvertSansMiseALAtelier() {
    assertThatThrownBy(() -> new PassageOuvert(SUIVI_DU_10_MAI_2026, null, EtatEnAtelier.EN_COURS))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("misALAtelierLe");
  }

  @Test
  void shouldNotBuildUnPassageOuvertSansEtat() {
    assertThatThrownBy(() -> new PassageOuvert(SUIVI_DU_10_MAI_2026, INSTANT_10_MAI_2026_A_7H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("etat");
  }

  @Test
  void shouldConstruireUnPassageClos() {
    var passage = new PassageClos(
      SUIVI_DU_10_MAI_2026,
      Instant.parse("2026-05-10T07:00:00Z"),
      Instant.parse("2026-05-10T18:00:00Z"),
      new AuteurDeLaCloture("leroy")
    );

    assertThat(passage.misALAtelierLe()).isEqualTo(INSTANT_10_MAI_2026_A_7H);
    assertThat(passage.clotureLe()).isEqualTo(INSTANT_10_MAI_2026_A_18H);
    assertThat(passage.cloturePar()).isEqualTo(AUTEUR_DE_LA_CLOTURE_LEROY);
  }

  @Test
  void shouldNotBuildUnPassageClosSansSuivi() {
    assertThatThrownBy(() -> new PassageClos(null, INSTANT_10_MAI_2026_A_7H, INSTANT_10_MAI_2026_A_18H, AUTEUR_DE_LA_CLOTURE_LEROY))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("suivi");
  }

  @Test
  void shouldNotBuildUnPassageClosSansMiseALAtelier() {
    assertThatThrownBy(() -> new PassageClos(SUIVI_DU_10_MAI_2026, null, INSTANT_10_MAI_2026_A_18H, AUTEUR_DE_LA_CLOTURE_LEROY))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("misALAtelierLe");
  }

  @Test
  void shouldNotBuildUnPassageClosSansCloture() {
    assertThatThrownBy(() -> new PassageClos(SUIVI_DU_10_MAI_2026, INSTANT_10_MAI_2026_A_7H, null, AUTEUR_DE_LA_CLOTURE_LEROY))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("clotureLe");
  }

  @Test
  void shouldNotBuildUnPassageClosSansAuteur() {
    assertThatThrownBy(() -> new PassageClos(SUIVI_DU_10_MAI_2026, INSTANT_10_MAI_2026_A_7H, INSTANT_10_MAI_2026_A_18H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cloturePar");
  }

  @Test
  void shouldNotBuildUnSuiviSansIdentifiant() {
    assertThatThrownBy(() -> new SuiviId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("suivi");
  }

  @Test
  void shouldNotBuildUnAuteurVide() {
    assertThatThrownBy(() -> new AuteurDeLaCloture(" ")).hasMessageContaining("auteur de la cloture");
  }
}

package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class AnnulationTest {

  @Test
  void shouldNotBuildWithoutAuteur() {
    assertThatThrownBy(() -> new Annulation(null, LE_11_MAI_2026_A_9H15, MOTIF_ERREUR_DE_SAISIE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("auteur");
  }

  @Test
  void shouldNotBuildWithoutDate() {
    assertThatThrownBy(() -> new Annulation(AUTEUR_LEROY, null, MOTIF_ERREUR_DE_SAISIE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date");
  }

  @Test
  void shouldNotBuildWithoutMotif() {
    assertThatThrownBy(() -> new Annulation(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("motif");
  }

  @Test
  void shouldTraceQuiAAnnuleEtQuand() {
    Annulation annulation = annulationParLeroy();

    assertThat(annulation.auteur()).isEqualTo(AUTEUR_LEROY);
    assertThat(annulation.date()).isEqualTo(LE_11_MAI_2026_A_9H15);
    assertThat(annulation.motif()).isEqualTo(MOTIF_ERREUR_DE_SAISIE);
  }
}

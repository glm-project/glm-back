package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourDeLaSemaineTest {

  private static final Plage DE_8H_A_12H = new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H));

  @Test
  void shouldNotBuildWithoutJour() {
    assertThatThrownBy(() -> new JourDeLaSemaine(null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jour");
  }

  @Test
  void shouldNotBuildWithoutPresence() {
    assertThatThrownBy(() -> new JourDeLaSemaine(LUNDI_11_MAI_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("presence");
  }

  @Test
  void shouldNotBuildWithNullPlage() {
    List<Plage> presence = Arrays.asList(DE_8H_A_12H, null);

    assertThatThrownBy(() -> new JourDeLaSemaine(LUNDI_11_MAI_2026, presence))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("presence");
  }

  @Test
  void shouldPorterSaDateEtSesFenetres() {
    JourDeLaSemaine lundi = new JourDeLaSemaine(LUNDI_11_MAI_2026, List.of(DE_8H_A_12H));

    assertThat(lundi.jour()).isEqualTo(LUNDI_11_MAI_2026);
    assertThat(lundi.presence()).containsExactly(DE_8H_A_12H);
  }

  @Test
  void shouldAccepterUnJourSansPresence() {
    assertThat(new JourDeLaSemaine(LUNDI_11_MAI_2026, List.of()).presence()).isEmpty();
  }
}

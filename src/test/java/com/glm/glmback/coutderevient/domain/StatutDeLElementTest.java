package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class StatutDeLElementTest {

  private static final EvaluationDuCout SANS_ACTIVITE_EN_COURS = new EvaluationDuCout(LE_13_MAI_A_8H, 0);

  @Test
  void shouldNotBuildWithoutTermineLe() {
    assertThatThrownBy(() -> new StatutDeLElement(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("termine le");
  }

  @Test
  void shouldNotJugerSansPassages() {
    assertThatThrownBy(() -> StatutDeLElement.de(null, SANS_ACTIVITE_EN_COURS))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("passages");
  }

  @Test
  void shouldNotJugerSansLecture() {
    assertThatThrownBy(() -> StatutDeLElement.de(List.of(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("lecture");
  }

  @Test
  void shouldEtreEnCoursSansPassage() {
    assertThat(StatutDeLElement.de(List.of(), SANS_ACTIVITE_EN_COURS)).isEqualTo(StatutDeLElement.EN_COURS);
  }

  @Test
  void shouldEtreEnCoursTantQuUnPassageEstOuvert() {
    List<PassageEnAtelier> passages = List.of(new PassageEnAtelier(Optional.of(LE_11_MAI_A_17H)), new PassageEnAtelier(Optional.empty()));

    assertThat(StatutDeLElement.de(passages, SANS_ACTIVITE_EN_COURS).termineLe()).isEmpty();
  }

  @Test
  void shouldEtreEnCoursTantQuUneActiviteEstEnCours() {
    List<PassageEnAtelier> passages = List.of(new PassageEnAtelier(Optional.of(LE_11_MAI_A_17H)));

    assertThat(StatutDeLElement.de(passages, new EvaluationDuCout(LE_13_MAI_A_8H, 1)).termineLe()).isEmpty();
  }

  @Test
  void shouldEtreTermineALaDerniereCloture() {
    List<PassageEnAtelier> passages = List.of(
      new PassageEnAtelier(Optional.of(LE_12_MAI_A_18H)),
      new PassageEnAtelier(Optional.of(LE_11_MAI_A_17H))
    );

    assertThat(StatutDeLElement.de(passages, SANS_ACTIVITE_EN_COURS).termineLe()).contains(LE_12_MAI_A_18H);
  }
}

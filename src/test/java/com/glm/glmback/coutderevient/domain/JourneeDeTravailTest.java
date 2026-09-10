package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeDeTravailTest {

  @Test
  void shouldNotBuildWithoutJournal() {
    assertThatThrownBy(() -> new JourneeDeTravail(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldRefuseImpossibleSequence() {
    assertThatThrownBy(() -> new JourneeDeTravail(List.of(departA(LE_11_MAI_A_17H)))).isExactlyInstanceOf(
      TransitionDePresenceInterditeException.class
    );
  }

  @Test
  void shouldHaveNoFenetreWithoutEvenement() {
    assertThat(new JourneeDeTravail(List.of()).fenetres()).isEmpty();
  }

  /**
   * La pause n'ote que son propre creux : la journee se lit en deux fenetres, et c'est ce qui scindera le travail de
   * midi en deux tranches valorisees.
   */
  @Test
  void shouldSplitFenetresOnPause() {
    assertThat(journeeDe8HA17HAvecPauseDeMidi().fenetres()).containsExactly(
      new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_12H)),
      new Plage(LE_11_MAI_A_13H, Optional.of(LE_11_MAI_A_17H))
    );
  }

  @Test
  void shouldKeepLastFenetreOpenWithoutDepart() {
    assertThat(journeeOuverteDepuis8H().fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.empty()));
  }

  @Test
  void shouldSortJournalByDateDeSurvenue() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(departA(LE_11_MAI_A_17H), arriveeA(LE_11_MAI_A_8H)));

    assertThat(journee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_17H)));
  }

  @Test
  void shouldContainInstantBetweenArriveeAndDepart() {
    assertThat(journeeDe8HA17HAvecPauseDeMidi().contient(LE_11_MAI_A_12H)).isTrue();
  }

  @Test
  void shouldNotContainInstantBeforeArrivee() {
    assertThat(journeeDe8HA17HAvecPauseDeMidi().contient(LE_11_MAI_A_8H.minusSeconds(1))).isFalse();
  }

  @Test
  void shouldNotContainInstantAfterDepart() {
    assertThat(journeeDe8HA17HAvecPauseDeMidi().contient(LE_12_MAI_A_8H)).isFalse();
  }

  /**
   * Une journee sans depart n'a pas de borne haute : l'operateur n'est simplement pas encore parti.
   */
  @Test
  void shouldContainAnyLaterInstantWhenStillOpen() {
    assertThat(journeeOuverteDepuis8H().contient(LE_12_MAI_A_8H)).isTrue();
  }

  @Test
  void shouldContainNothingWithoutEvenement() {
    assertThat(new JourneeDeTravail(List.of()).contient(LE_11_MAI_A_8H)).isFalse();
  }
}

package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDAtelierCriteriaTest {

  @Test
  void shouldNotBuildWithoutPeriode() {
    assertThatThrownBy(() -> new SuiviDAtelierCriteria(null, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("periode");
  }

  @Test
  void shouldNotBuildWithoutEtats() {
    assertThatThrownBy(() -> new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("etats");
  }

  /**
   * L'ecran des operateurs : tous les elements actifs, sans notion de date.
   */
  @Test
  void shouldMatchSansAucunePeriode() {
    assertThat(new SuiviDAtelierCriteria(Optional.empty(), Set.of()).matches(suiviDAtelierEngage())).isTrue();
  }

  @Test
  void shouldMatchAnyEtatWhenAucunEtatDemande() {
    assertThat(new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), Set.of()).matches(suiviDAtelierEngage())).isTrue();
  }

  @Test
  void shouldNotMatchSuiviEngageHorsDeLaPeriode() {
    Periode lendemain = new Periode(LE_11_MAI_2026_A_9H15, LE_11_MAI_2026_A_9H15);

    assertThat(new SuiviDAtelierCriteria(Optional.of(lendemain), Set.of()).matches(suiviDAtelierEngage())).isFalse();
  }

  @Test
  void shouldMatchDemandedEtat() {
    assertThat(
      new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), Set.of(EtatDAtelier.EN_ATTENTE)).matches(suiviDAtelierEngage())
    ).isTrue();
  }

  @Test
  void shouldNotMatchOtherEtat() {
    assertThat(
      new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), Set.of(EtatDAtelier.EN_COURS)).matches(suiviDAtelierEngage())
    ).isFalse();
  }
}

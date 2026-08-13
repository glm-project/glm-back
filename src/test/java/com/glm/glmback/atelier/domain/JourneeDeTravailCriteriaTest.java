package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeDeTravailCriteriaTest {

  @Test
  void shouldNotBuildWithoutPeriode() {
    assertThatThrownBy(() -> new JourneeDeTravailCriteria(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("periode");
  }

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new JourneeDeTravailCriteria(Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldMatchAnyJourneeSansCritere() {
    assertThat(new JourneeDeTravailCriteria(Optional.empty(), Optional.empty()).matches(journeeDeDupontOuverteA7H())).isTrue();
  }

  @Test
  void shouldMatchJourneeDeLOperateurDemande() {
    assertThat(
      new JourneeDeTravailCriteria(Optional.empty(), Optional.of(OPERATEUR_ID_DUPONT)).matches(journeeDeDupontOuverteA7H())
    ).isTrue();
  }

  @Test
  void shouldNotMatchJourneeDUnAutreOperateur() {
    assertThat(
      new JourneeDeTravailCriteria(Optional.empty(), Optional.of(OPERATEUR_ID_MARTIN)).matches(journeeDeDupontOuverteA7H())
    ).isFalse();
  }

  @Test
  void shouldMatchJourneeCommenceeDansLaPeriode() {
    assertThat(
      new JourneeDeTravailCriteria(Optional.of(journeeDu10Mai2026()), Optional.empty()).matches(journeeDeDupontOuverteA7H())
    ).isTrue();
  }

  @Test
  void shouldNotMatchJourneeCommenceeHorsDeLaPeriode() {
    Periode lendemain = new Periode(LE_11_MAI_2026_A_9H15, LE_11_MAI_2026_A_9H15);

    assertThat(new JourneeDeTravailCriteria(Optional.of(lendemain), Optional.empty()).matches(journeeDeDupontOuverteA7H())).isFalse();
  }

  @Test
  void shouldNotMatchJourneeSansAucunPointageQuandUnePeriodeEstDemandee() {
    JourneeDeTravail vide = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_DUPONT);

    assertThat(new JourneeDeTravailCriteria(Optional.of(journeeDu10Mai2026()), Optional.empty()).matches(vide)).isFalse();
  }
}

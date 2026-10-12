package com.glm.glmback.syntheseheures.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@UnitTest
class MoisCalendaireTest {

  @Test
  void shouldNotBuildWithMoisAvantJanvier() {
    assertThatThrownBy(() -> new MoisCalendaire(2026, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("mois");
  }

  @Test
  void shouldNotBuildWithMoisApresDecembre() {
    assertThatThrownBy(() -> new MoisCalendaire(2026, 13))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("mois");
  }

  @Test
  void shouldNotBuildWithAnneeAberranteEnDessous() {
    assertThatThrownBy(() -> new MoisCalendaire(1999, 9))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("annee");
  }

  @Test
  void shouldNotBuildWithAnneeAberranteAuDessus() {
    assertThatThrownBy(() -> new MoisCalendaire(3000, 9))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("annee");
  }

  @Test
  void shouldAllerDuPremierAuDernierJourDuMois() {
    MoisCalendaire septembre = new MoisCalendaire(2026, 9);

    assertThat(septembre.premierJour()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(septembre.dernierJour()).isEqualTo(LocalDate.of(2026, 9, 30));
  }

  @Test
  void shouldFinirLeVingtNeufFevrierDUneAnneeBissextile() {
    assertThat(new MoisCalendaire(2028, 2).dernierJour()).isEqualTo(LocalDate.of(2028, 2, 29));
  }
}

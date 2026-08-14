package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@UnitTest
class SemaineCalendaireTest {

  @Test
  void shouldNotBuildWithNumeroAvantLaPremiereSemaine() {
    assertThatThrownBy(() -> new SemaineCalendaire(2026, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("numero de semaine");
  }

  @Test
  void shouldNotBuildWithNumeroApresLaDerniereSemaine() {
    assertThatThrownBy(() -> new SemaineCalendaire(2026, 54))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("numero de semaine");
  }

  @Test
  void shouldNotBuildWithAnneeAberranteEnDessous() {
    assertThatThrownBy(() -> new SemaineCalendaire(1999, 20))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("annee");
  }

  @Test
  void shouldNotBuildWithAnneeAberranteAuDessus() {
    assertThatThrownBy(() -> new SemaineCalendaire(3000, 20))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("annee");
  }

  @Test
  void shouldCommencerLeLundiDeLaSemaine() {
    assertThat(new SemaineCalendaire(2026, 20).lundi()).isEqualTo(LocalDate.of(2026, 5, 11));
  }

  /**
   * L'annee ISO n'est pas l'annee civile : la semaine 1 de 2026 commence le 29 decembre 2025.
   */
  @Test
  void shouldCommencerAvantLAnneeCivileQuandLaPremiereSemaineDeborde() {
    assertThat(new SemaineCalendaire(2026, 1).lundi()).isEqualTo(LocalDate.of(2025, 12, 29));
  }

  /**
   * 2026 commence un jeudi, donc compte 53 semaines : la derniere finit apres le 31 decembre.
   */
  @Test
  void shouldPorterLaCinquanteTroisiemeSemaineDesAnneesQuiEnComptent() {
    assertThat(new SemaineCalendaire(2026, 53).lundi()).isEqualTo(LocalDate.of(2026, 12, 28));
  }
}

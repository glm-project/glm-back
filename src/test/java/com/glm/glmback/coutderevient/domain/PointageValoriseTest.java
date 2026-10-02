package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageValoriseTest {

  private static final TrancheDActivite FRAISAGE_DE_9H_A_11H = new TrancheDActivite(
    ACTIVITE_FRAISAGE,
    new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H)
  );

  @Test
  void shouldNotBuildWithoutTranche() {
    assertThatThrownBy(() -> new PointageValorise(null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("tranche");
  }

  @Test
  void shouldNotBuildWithoutParts() {
    assertThatThrownBy(() -> new PointageValorise(FRAISAGE_DE_9H_A_11H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("parts");
  }

  /**
   * Seul de 9 h a 10 h, puis de front avec un tour : 20,00 + 10,00 EUR de main d'oeuvre, et deux heures de fraiseuse
   * entieres a 45 EUR.
   */
  @Test
  void shouldSumItsPartsAndCostTheWholeMachine() {
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(
      List.of(FRAISAGE_DE_9H_A_11H, new TrancheDActivite(ACTIVITE_TOURNAGE, new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_12H)))
    );

    PointageValorise pointage = new PointageValorise(FRAISAGE_DE_9H_A_11H, charge.decoupe(FRAISAGE_DE_9H_A_11H));

    assertThat(pointage.activite()).isEqualTo(ACTIVITE_FRAISAGE);
    assertThat(pointage.debut()).isEqualTo(LE_11_MAI_A_9H);
    assertThat(pointage.machine()).isEqualTo(new Montant(new BigDecimal("90.00")));
    assertThat(pointage.mainDOeuvre()).contains(new Montant(new BigDecimal("30.00")));
    assertThat(pointage.partageInconnu()).isFalse();
    assertThat(pointage.finAutomatique()).isFalse();
  }

  @Test
  void shouldNotSumAShareWaitingForAnUnknownDiviseur() {
    ZoneIncertaine tour = new ZoneIncertaine(
      activiteInterpreteeDeTournage(new Plage(LE_11_MAI_A_10H, Optional.empty())),
      new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_12H)
    );
    ChargeDeLOperateur charge = ChargeDeLOperateur.de(List.of(FRAISAGE_DE_9H_A_11H), List.of(tour));

    PointageValorise pointage = new PointageValorise(FRAISAGE_DE_9H_A_11H, charge.decoupe(FRAISAGE_DE_9H_A_11H));

    assertThat(pointage.partageInconnu()).isTrue();
    assertThat(pointage.mainDOeuvre()).isEmpty();
  }

  @Test
  void shouldTellAnAutomaticEnd() {
    TrancheDActivite arreteeAutomatiquement = new TrancheDActivite(ACTIVITE_FRAISAGE, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H), true);

    assertThat(new PointageValorise(arreteeAutomatiquement, List.of()).finAutomatique()).isTrue();
  }
}

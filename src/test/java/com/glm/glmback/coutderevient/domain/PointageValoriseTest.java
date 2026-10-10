package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import java.util.List;
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
    assertThat(pointage.mainDOeuvre()).isEqualTo(new Montant(new BigDecimal("30.00")));
    assertThat(pointage.finAutomatique()).isFalse();
    assertThat(pointage.fin()).isEqualTo(LE_11_MAI_A_11H);
    assertThat(pointage.duree()).isEqualTo(DureeTotale.de(java.time.Duration.ofHours(2)));
    assertThat(pointage.cout()).isEqualTo(new Cout(new Montant(new BigDecimal("90.00")), new Montant(new BigDecimal("30.00"))));
    assertThat(pointage.anomalies()).isEmpty();
  }

  @Test
  void shouldTellAnAutomaticEnd() {
    TrancheDActivite arreteeAutomatiquement = new TrancheDActivite(ACTIVITE_FRAISAGE, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H), true);

    PointageValorise pointage = new PointageValorise(arreteeAutomatiquement, List.of());

    assertThat(pointage.finAutomatique()).isTrue();
    assertThat(pointage.anomalies()).containsExactly(AnomalieDuPointage.FIN_AUTOMATIQUE);
  }

  @Test
  void shouldNotMissATarifWhenTheRatesAreKnown() {
    assertThat(pointage(ACTIVITE_FRAISAGE).tarifManquant()).isFalse();
  }

  @Test
  void shouldMissATarifWithoutTauxHoraire() {
    assertThat(pointage(ACTIVITE_FRAISAGE_SANS_TAUX_HORAIRE).tarifManquant()).isTrue();
  }

  @Test
  void shouldMissATarifWithoutCoutHoraireOnAPoste() {
    assertThat(pointage(ACTIVITE_FRAISAGE_SANS_COUT_HORAIRE).tarifManquant()).isTrue();
  }

  @Test
  void shouldAcceptAPosteAt0Euro() {
    assertThat(pointage(ACTIVITE_FRAISAGE_A_0_EURO).tarifManquant()).isFalse();
  }

  @Test
  void shouldNotMissACoutHoraireWithoutPoste() {
    assertThat(pointage(ACTIVITE_SANS_POSTE).tarifManquant()).isFalse();
  }

  private static PointageValorise pointage(Activite activite) {
    return new PointageValorise(new TrancheDActivite(activite, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H)), List.of());
  }
}

package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class TrancheDActiviteTest {

  private static final Activite TOURNAGE_DE_DUPONT = Activite.builder()
    .operateur(OPERATEUR_ID_DUPONT)
    .poste(Optional.of(POSTE_ID_TOUR))
    .nature(Optional.of(NATURE_TOURNAGE))
    .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
    .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
    .categorie(CategorieDActivite.TRAVAIL);

  @Test
  void shouldNotBuildWithoutActivite() {
    assertThatThrownBy(() -> new TrancheDActivite(null, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H)))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activite");
  }

  @Test
  void shouldNotBuildWithoutPeriode() {
    assertThatThrownBy(() -> new TrancheDActivite(TOURNAGE_DE_DUPONT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("periode");
  }

  @Test
  void shouldMeasureDuree() {
    assertThat(new TrancheDActivite(TOURNAGE_DE_DUPONT, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H)).duree()).isEqualTo(
      Duration.ofHours(2)
    );
  }

  @Test
  void shouldReduceToPeriode() {
    TrancheDActivite tranche = new TrancheDActivite(TOURNAGE_DE_DUPONT, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_12H));

    assertThat(tranche.reduiteA(new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H))).contains(
      new TrancheDActivite(TOURNAGE_DE_DUPONT, new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H))
    );
  }

  @Test
  void shouldNotReduceToDisjointPeriode() {
    TrancheDActivite tranche = new TrancheDActivite(TOURNAGE_DE_DUPONT, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H));

    assertThat(tranche.reduiteA(new Periode(LE_11_MAI_A_11H, LE_11_MAI_A_12H))).isEmpty();
  }

  @Test
  void shouldExposeOperateurOfItsActivite() {
    TrancheDActivite tranche = new TrancheDActivite(TOURNAGE_DE_DUPONT, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H));

    assertThat(tranche.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
  }
}

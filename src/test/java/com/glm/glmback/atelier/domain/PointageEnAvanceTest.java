package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageEnAvanceTest {

  @Test
  void shouldNotBuildWithoutPointage() {
    assertThatThrownBy(() -> new PointageEnAvance(null, LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("pointage");
  }

  @Test
  void shouldNotBuildWithoutDateDeReception() {
    assertThatThrownBy(() -> new PointageEnAvance(pointageAnnonce(Optional.of(LE_10_MAI_2026_A_9H)), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDeReception");
  }

  @Test
  void shouldNotBuildAPointageSansHeureAnnoncee() {
    assertThatThrownBy(() -> new PointageEnAvance(pointageAnnonce(Optional.empty()), LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDeSurvenue");
  }

  @Test
  void shouldNotBuildAPointageAnnonceAvantSaReception() {
    assertThatThrownBy(() -> new PointageEnAvance(pointageAnnonce(Optional.of(LE_10_MAI_2026_A_8H)), LE_10_MAI_2026_A_8H))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("dateDeSurvenue");
  }

  @Test
  void shouldGarderLHeureAnnonceeEtLaRaison() {
    PointageEnAvance enAvance = new PointageEnAvance(pointageAnnonce(Optional.of(LE_10_MAI_2026_A_9H)), LE_10_MAI_2026_A_8H);

    assertThat(enAvance.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_9H);
    assertThat(enAvance.dateDeReception()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(enAvance.raison()).isEqualTo(RaisonDePointageIgnore.HORLOGE_EN_AVANCE);
  }

  private static PointageAEnregistrer pointageAnnonce(Optional<Instant> dateDeSurvenue) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(SuiviDAtelierId.newId())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .dateDeSurvenue(dateDeSurvenue)
      .evenement(EvenementDAtelierId.newId());
  }
}

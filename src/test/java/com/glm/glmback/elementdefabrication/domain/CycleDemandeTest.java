package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class CycleDemandeTest {

  private static final Periode LE_10_MAI_2026 = new Periode(Instant.parse("2026-05-10T00:00:00Z"), Instant.parse("2026-05-10T23:59:59Z"));
  private static final Periode LE_11_MAI_2026 = new Periode(Instant.parse("2026-05-11T00:00:00Z"), Instant.parse("2026-05-11T23:59:59Z"));

  @Test
  void shouldRetenirUnElementCloturePendantLaPeriode() {
    var demande = new CycleDemande(CycleDeVie.CLOTURE, Optional.of(LE_10_MAI_2026));

    assertThat(
      demande.matches(new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.of(passageClosParLeroyLe10Mai2026())))
    ).isTrue();
  }

  @Test
  void shouldEcarterUnElementClotureHorsDeLaPeriode() {
    var demande = new CycleDemande(CycleDeVie.CLOTURE, Optional.of(LE_11_MAI_2026));

    assertThat(
      demande.matches(new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.of(passageClosParLeroyLe10Mai2026())))
    ).isFalse();
  }

  @Test
  void shouldEcarterUnElementDUnAutreCycle() {
    var demande = new CycleDemande(CycleDeVie.CLOTURE, Optional.empty());

    assertThat(
      demande.matches(new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.of(passageEnCoursDepuisLe10Mai2026())))
    ).isFalse();
  }

  @Test
  void shouldEcarterUnElementNonClotureMemeAvecUnePeriode() {
    var lu = new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.of(passageEnCoursDepuisLe10Mai2026()));

    assertThat(new CycleDemande(CycleDeVie.CLOTURE, Optional.of(LE_10_MAI_2026)).matches(lu)).isFalse();
  }

  @Test
  void shouldRefuserUnePeriodeDeClotureHorsDesClotures() {
    assertThatThrownBy(() -> new CycleDemande(CycleDeVie.EN_ATELIER, Optional.of(LE_10_MAI_2026))).isExactlyInstanceOf(
      ClotureHorsDesCloturesException.class
    );
  }

  @Test
  void shouldRefuserUnePeriodeDeClotureSansCycle() {
    assertThatThrownBy(() -> CycleDemande.de(Optional.empty(), Optional.of(LE_10_MAI_2026))).isExactlyInstanceOf(
      ClotureHorsDesCloturesException.class
    );
  }

  @Test
  void shouldNeRienDemanderSansCycle() {
    assertThat(CycleDemande.de(Optional.empty(), Optional.empty())).isEmpty();
  }

  @Test
  void shouldDemanderLeCycleDonne() {
    assertThat(CycleDemande.de(Optional.of(CycleDeVie.CLOTURE), Optional.of(LE_10_MAI_2026))).contains(
      new CycleDemande(CycleDeVie.CLOTURE, Optional.of(LE_10_MAI_2026))
    );
  }

  @Test
  void shouldNotBuildWithoutCycle() {
    assertThatThrownBy(() -> new CycleDemande(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cycle");
  }

  @Test
  void shouldNotBuildWithoutCloture() {
    assertThatThrownBy(() -> new CycleDemande(CycleDeVie.CLOTURE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cloture");
  }
}

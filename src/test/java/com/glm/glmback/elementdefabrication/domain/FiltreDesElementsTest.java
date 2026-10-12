package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class FiltreDesElementsTest {

  @Test
  void shouldToutRetenirSansFiltre() {
    assertThat(FiltreDesElements.aucun().matches(elementDeFabricationOrdre1015())).isTrue();
  }

  @Test
  void shouldRetenirLaCategorieDemandee() {
    var filtre = new FiltreDesElements(Optional.empty(), Optional.of(CATEGORIE_OF), Optional.empty());

    assertThat(filtre.matches(elementDeFabricationOrdre1015())).isTrue();
    assertThat(filtre.matches(elementDeFabricationMoule2456())).isFalse();
  }

  @Test
  void shouldRetenirLeTexteCherche() {
    var filtre = new FiltreDesElements(Optional.empty(), Optional.empty(), Optional.of(new Recherche("2456")));

    assertThat(filtre.matches(elementDeFabricationMoule2456())).isTrue();
    assertThat(filtre.matches(elementDeFabricationOrdre1015())).isFalse();
  }

  @Test
  void shouldRetenirLaPeriodeDeCreation() {
    var filtre = new FiltreDesElements(Optional.of(premierTrimestre2026()), Optional.empty(), Optional.empty());

    assertThat(filtre.matches(elementDeFabricationOrdre1015CreeLe(LE_15_JANVIER_2026))).isTrue();
    assertThat(filtre.matches(elementDeFabricationOrdre1015CreeLe(LE_31_MARS_2026.plusSeconds(1)))).isFalse();
  }

  @Test
  void shouldNotBuildWithoutCreation() {
    assertThatThrownBy(() -> new FiltreDesElements(null, Optional.empty(), Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("creation");
  }

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() -> new FiltreDesElements(Optional.empty(), null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldNotBuildWithoutRecherche() {
    assertThatThrownBy(() -> new FiltreDesElements(Optional.empty(), Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("recherche");
  }
}

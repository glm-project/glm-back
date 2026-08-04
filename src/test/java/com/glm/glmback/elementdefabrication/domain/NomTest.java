package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class NomTest {

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new Nom(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithBlankNom() {
    assertThatThrownBy(() -> new Nom(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldBuildNomFromPrefixeAnneeAndNumero() {
    assertThat(OF_2026_000001.value()).isEqualTo("OF-2026-000001");
    assertThat(PRD_2026_000001.value()).isEqualTo("PRD-2026-000001");
  }

  @Test
  void shouldBuildNomFromAnyPrefixe() {
    assertThat(Nom.de(new Prefixe("FAB"), ANNEE_2026, 1).value()).isEqualTo("FAB-2026-000001");
  }

  @Test
  void shouldPadNumeroOverSixDigits() {
    assertThat(Nom.de(PREFIXE_OF, ANNEE_2026, 123456).value()).isEqualTo("OF-2026-123456");
  }
}

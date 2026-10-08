package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
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
  void shouldNotBuildWithNomOutOfPattern() {
    assertThatThrownBy(() -> new Nom("of-2026-1"))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldBuildNomFromCategorieAnneeAndCompteur() {
    assertThat(OF_2026_000001.value()).isEqualTo("OF-2026-000001");
    assertThat(MOULE_2026_000001.value()).isEqualTo("MOULE-2026-000001");
  }

  @Test
  void shouldBuildNomFromAnyCategorie() {
    assertThat(Nom.of(new Categorie("FAB"), ANNEE_2026, 1).value()).isEqualTo("FAB-2026-000001");
  }

  @Test
  void shouldPadCompteurOverSixDigits() {
    assertThat(Nom.of(CATEGORIE_OF, ANNEE_2026, 123456).value()).isEqualTo("OF-2026-123456");
  }
}

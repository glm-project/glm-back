package com.glm.glmback.shared.elementtype.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class LegacyElementTypeTest {

  @Test
  void shouldTranslateOrdreDeFabricationToOf() {
    assertThat(LegacyElementType.toCategory("ORDRE_DE_FABRICATION")).contains("OF");
  }

  @Test
  void shouldTranslateProduitToMoule() {
    assertThat(LegacyElementType.toCategory("PRODUIT")).contains("MOULE");
  }

  @Test
  void shouldNotTranslateMissingType() {
    assertThat(LegacyElementType.toCategory(null)).isEmpty();
  }

  @Test
  void shouldNotTranslateUnknownType() {
    assertThat(LegacyElementType.toCategory("ARTICLE")).isEmpty();
  }

  @Test
  void shouldReadOfAsOrdreDeFabrication() {
    assertThat(LegacyElementType.fromCategory("OF")).isEqualTo("ORDRE_DE_FABRICATION");
  }

  @Test
  void shouldReadAnyOtherCategorieAsProduit() {
    assertThat(LegacyElementType.fromCategory("MOULE")).isEqualTo("PRODUIT");
    assertThat(LegacyElementType.fromCategory("PIECE")).isEqualTo("PRODUIT");
  }
}

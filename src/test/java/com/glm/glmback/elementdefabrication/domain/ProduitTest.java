package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ProduitTest {

  @Test
  void shouldNotBuildWithoutId() {
    Fiche fiche = ficheCarterMoteur();

    assertThatThrownBy(() -> new Produit(null, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutFiche() {
    ProduitId id = ProduitId.newId();

    assertThatThrownBy(() -> new Produit(id, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fiche");
  }

  @Test
  void shouldBuildProduitFromStepBuilder() {
    ProduitId id = ProduitId.newId();

    Produit produit = Produit.builder()
      .id(id)
      .titre(titreCarterMoteur())
      .description(descriptionCarterEnFonte())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(produit.id()).isEqualTo(id);
    assertThat(produit.fiche()).isEqualTo(ficheCarterMoteur());
  }
}

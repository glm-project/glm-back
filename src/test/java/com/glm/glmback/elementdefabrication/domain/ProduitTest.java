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

    assertThatThrownBy(() -> new Produit(null, PRD_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    ProduitId id = ProduitId.newId();
    Fiche fiche = ficheCarterMoteur();

    assertThatThrownBy(() -> new Produit(id, null, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutFiche() {
    ProduitId id = ProduitId.newId();

    assertThatThrownBy(() -> new Produit(id, PRD_2026_000001, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fiche");
  }

  @Test
  void shouldBuildProduitFromPrimitives() {
    ProduitId id = ProduitId.newId();

    Produit produit = Produit.builder()
      .id(id)
      .prefixe("PRD")
      .annee(2026)
      .compteur(1)
      .titre("Carter moteur")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(produit.id()).isEqualTo(id);
    assertThat(produit.nom()).isEqualTo(PRD_2026_000001);
    assertThat(produit.fiche()).isEqualTo(ficheCarterMoteur());
  }
}

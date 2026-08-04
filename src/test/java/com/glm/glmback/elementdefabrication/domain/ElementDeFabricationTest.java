package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationTest {

  @Test
  void shouldReadFicheFromOrdreDeFabrication() {
    OrdreDeFabricationId id = OrdreDeFabricationId.newId();

    ElementDeFabrication element = OrdreDeFabrication.builder()
      .id(id)
      .titre(titreAssemblageCarter())
      .description(descriptionCarterEnFonte())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_20_FEVRIER_2026);

    assertThat(element.id()).isEqualTo(id);
    assertThat(element.titre()).isEqualTo(titreAssemblageCarter());
    assertThat(element.description()).isEqualTo(descriptionCarterEnFonte());
    assertThat(element.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(element.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }

  @Test
  void shouldReadFicheFromProduit() {
    ProduitId id = ProduitId.newId();

    ElementDeFabrication element = Produit.builder()
      .id(id)
      .titre(titreCarterMoteur())
      .description(descriptionCarterEnFonte())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_20_FEVRIER_2026);

    assertThat(element.id()).isEqualTo(id);
    assertThat(element.titre()).isEqualTo(titreCarterMoteur());
    assertThat(element.description()).isEqualTo(descriptionCarterEnFonte());
    assertThat(element.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(element.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }

  @Test
  void shouldNotShareIdentityBetweenOrdreDeFabricationAndProduit() {
    OrdreDeFabricationId ordreId = OrdreDeFabricationId.newId();

    ElementDeFabricationId produitId = new ProduitId(ordreId.uuid());

    assertThat(produitId).isNotEqualTo(ordreId);
  }
}

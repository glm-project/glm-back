package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationToCreateTest {

  @Test
  void shouldNotBuildOrdreDeFabricationWithoutTitre() {
    assertThatThrownBy(() -> new OrdreDeFabricationToCreate(null, descriptionCarterEnFonte()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldNotBuildOrdreDeFabricationWithoutDescription() {
    assertThatThrownBy(() -> new OrdreDeFabricationToCreate(titreAssemblageCarter(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldNotBuildProduitWithoutTitre() {
    assertThatThrownBy(() -> new ProduitToCreate(null, descriptionCarterEnFonte()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldNotBuildProduitWithoutDescription() {
    assertThatThrownBy(() -> new ProduitToCreate(titreCarterMoteur(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldReadOrdreDeFabricationToCreate() {
    ElementDeFabricationToCreate toCreate = ordreDeFabricationToCreateAssemblageCarter();

    assertThat(toCreate.titre()).isEqualTo(titreAssemblageCarter());
    assertThat(toCreate.description()).isEqualTo(descriptionCarterEnFonte());
  }

  @Test
  void shouldReadProduitToCreate() {
    ElementDeFabricationToCreate toCreate = produitToCreateCarterMoteur();

    assertThat(toCreate.titre()).isEqualTo(titreCarterMoteur());
    assertThat(toCreate.description()).isEqualTo(descriptionCarterEnFonte());
  }
}

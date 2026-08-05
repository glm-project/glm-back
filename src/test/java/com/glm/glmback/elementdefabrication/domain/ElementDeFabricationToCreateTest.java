package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationToCreateTest {

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new ElementDeFabricationToCreate(null, titreAssemblageCarter(), descriptionCarterEnFonte()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutTitre() {
    assertThatThrownBy(() ->
      new ElementDeFabricationToCreate(TypeDElementDeFabrication.ORDRE_DE_FABRICATION, null, descriptionCarterEnFonte())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    assertThatThrownBy(() ->
      new ElementDeFabricationToCreate(TypeDElementDeFabrication.ORDRE_DE_FABRICATION, titreAssemblageCarter(), null)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldReadElementDeFabricationToCreate() {
    ElementDeFabricationToCreate toCreate = elementDeFabricationToCreateCarterMoteur();

    assertThat(toCreate.type()).isEqualTo(TypeDElementDeFabrication.PRODUIT);
    assertThat(toCreate.titre()).isEqualTo(titreCarterMoteur());
    assertThat(toCreate.description()).isEqualTo(descriptionCarterEnFonte());
  }

  @Test
  void shouldBuildElementDeFabricationToCreateFromPrimitives() {
    ElementDeFabricationToCreate toCreate = new ElementDeFabricationToCreate(
      TypeDElementDeFabrication.ORDRE_DE_FABRICATION,
      "Assemblage carter",
      "Carter en fonte"
    );

    assertThat(toCreate).isEqualTo(elementDeFabricationToCreateAssemblageCarter());
  }
}

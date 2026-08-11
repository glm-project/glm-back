package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationToCreateTest {

  @Test
  void shouldNotBuildWithoutType() {
    Optional<Reference> reference = Optional.of(reference1015());
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new ElementDeFabricationToCreate(null, reference, description))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutReference() {
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new ElementDeFabricationToCreate(TypeDElementDeFabrication.ORDRE_DE_FABRICATION, null, description))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    Optional<Reference> reference = Optional.of(reference1015());

    assertThatThrownBy(() -> new ElementDeFabricationToCreate(TypeDElementDeFabrication.ORDRE_DE_FABRICATION, reference, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldReadElementDeFabricationToCreate() {
    ElementDeFabricationToCreate toCreate = elementDeFabricationToCreateProduit2456();

    assertThat(toCreate.type()).isEqualTo(TypeDElementDeFabrication.PRODUIT);
    assertThat(toCreate.reference()).contains(reference2456());
    assertThat(toCreate.description()).contains(descriptionCarterEnFonte());
  }

  @Test
  void shouldBuildElementDeFabricationToCreateFromPrimitives() {
    ElementDeFabricationToCreate toCreate = new ElementDeFabricationToCreate(
      TypeDElementDeFabrication.ORDRE_DE_FABRICATION,
      "1015",
      "Carter en fonte"
    );

    assertThat(toCreate).isEqualTo(elementDeFabricationToCreateOrdre1015());
  }

  @Test
  void shouldBuildElementDeFabricationToCreateWithoutReferenceNorDescription() {
    ElementDeFabricationToCreate toCreate = new ElementDeFabricationToCreate(TypeDElementDeFabrication.PRODUIT, (String) null, null);

    assertThat(toCreate.reference()).isEmpty();
    assertThat(toCreate.description()).isEmpty();
  }
}

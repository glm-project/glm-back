package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationToUpdateTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new ElementDeFabricationToUpdate(null, titreAssemblageCarter(), descriptionCarterEnFonte()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutTitre() {
    ElementDeFabricationId id = OrdreDeFabricationId.newId();

    assertThatThrownBy(() -> new ElementDeFabricationToUpdate(id, null, descriptionCarterEnFonte()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    ElementDeFabricationId id = OrdreDeFabricationId.newId();

    assertThatThrownBy(() -> new ElementDeFabricationToUpdate(id, titreAssemblageCarterRevise(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldReadElementDeFabricationToUpdate() {
    ElementDeFabricationId id = OrdreDeFabricationId.newId();

    ElementDeFabricationToUpdate toUpdate = elementDeFabricationToUpdateAssemblageCarterRevise(id);

    assertThat(toUpdate.id()).isEqualTo(id);
    assertThat(toUpdate.titre()).isEqualTo(titreAssemblageCarterRevise());
    assertThat(toUpdate.description()).isEqualTo(descriptionCarterEnFonte());
  }

  @Test
  void shouldBuildElementDeFabricationToUpdateFromPrimitives() {
    ElementDeFabricationId id = OrdreDeFabricationId.newId();

    ElementDeFabricationToUpdate toUpdate = new ElementDeFabricationToUpdate(id, "Assemblage carter revise", "Carter en fonte");

    assertThat(toUpdate).isEqualTo(elementDeFabricationToUpdateAssemblageCarterRevise(id));
  }
}

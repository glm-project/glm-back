package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationToUpdateTest {

  @Test
  void shouldNotBuildWithoutId() {
    Optional<Reference> reference = Optional.of(reference1015());
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new ElementDeFabricationToUpdate(null, reference, description))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutReference() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new ElementDeFabricationToUpdate(id, null, description))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Optional<Reference> reference = Optional.of(reference1017());

    assertThatThrownBy(() -> new ElementDeFabricationToUpdate(id, reference, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldReadElementDeFabricationToUpdate() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();

    ElementDeFabricationToUpdate toUpdate = elementDeFabricationToUpdate1017(id);

    assertThat(toUpdate.id()).isEqualTo(id);
    assertThat(toUpdate.reference()).contains(reference1017());
    assertThat(toUpdate.description()).contains(descriptionCarterEnFonte());
  }

  @Test
  void shouldBuildElementDeFabricationToUpdateFromPrimitives() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();

    ElementDeFabricationToUpdate toUpdate = new ElementDeFabricationToUpdate(id, "1017", "Carter en fonte");

    assertThat(toUpdate).isEqualTo(elementDeFabricationToUpdate1017(id));
  }

  @Test
  void shouldBuildElementDeFabricationToUpdateWithoutReferenceNorDescription() {
    ElementDeFabricationToUpdate toUpdate = new ElementDeFabricationToUpdate(ElementDeFabricationId.newId(), (String) null, null);

    assertThat(toUpdate.reference()).isEmpty();
    assertThat(toUpdate.description()).isEmpty();
  }
}

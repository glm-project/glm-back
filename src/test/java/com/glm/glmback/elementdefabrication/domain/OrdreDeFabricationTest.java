package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class OrdreDeFabricationTest {

  @Test
  void shouldNotBuildWithoutId() {
    Fiche fiche = ficheAssemblageCarter();

    assertThatThrownBy(() -> new OrdreDeFabrication(null, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutFiche() {
    OrdreDeFabricationId id = OrdreDeFabricationId.newId();

    assertThatThrownBy(() -> new OrdreDeFabrication(id, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fiche");
  }

  @Test
  void shouldBuildOrdreDeFabricationFromStepBuilder() {
    OrdreDeFabricationId id = OrdreDeFabricationId.newId();

    OrdreDeFabrication ordre = OrdreDeFabrication.builder()
      .id(id)
      .titre(titreAssemblageCarter())
      .description(descriptionCarterEnFonte())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(ordre.id()).isEqualTo(id);
    assertThat(ordre.fiche()).isEqualTo(ficheAssemblageCarter());
  }
}

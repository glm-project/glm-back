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

    assertThatThrownBy(() -> new OrdreDeFabrication(null, OF_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    OrdreDeFabricationId id = OrdreDeFabricationId.newId();
    Fiche fiche = ficheAssemblageCarter();

    assertThatThrownBy(() -> new OrdreDeFabrication(id, null, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutFiche() {
    OrdreDeFabricationId id = OrdreDeFabricationId.newId();

    assertThatThrownBy(() -> new OrdreDeFabrication(id, OF_2026_000001, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fiche");
  }

  @Test
  void shouldBuildOrdreDeFabricationFromStepBuilder() {
    OrdreDeFabricationId id = OrdreDeFabricationId.newId();

    OrdreDeFabrication ordre = OrdreDeFabrication.builder()
      .id(id)
      .nom(OF_2026_000001)
      .titre(titreAssemblageCarter())
      .description(descriptionCarterEnFonte())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(ordre.id()).isEqualTo(id);
    assertThat(ordre.nom()).isEqualTo(OF_2026_000001);
    assertThat(ordre.fiche()).isEqualTo(ficheAssemblageCarter());
  }
}

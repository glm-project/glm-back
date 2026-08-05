package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationTest {

  @Test
  void shouldNotBuildWithoutId() {
    Fiche fiche = ficheAssemblageCarter();

    assertThatThrownBy(() -> new ElementDeFabrication(null, TypeDElementDeFabrication.ORDRE_DE_FABRICATION, OF_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutType() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Fiche fiche = ficheAssemblageCarter();

    assertThatThrownBy(() -> new ElementDeFabrication(id, null, OF_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Fiche fiche = ficheAssemblageCarter();

    assertThatThrownBy(() -> new ElementDeFabrication(id, TypeDElementDeFabrication.ORDRE_DE_FABRICATION, null, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutFiche() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();

    assertThatThrownBy(() -> new ElementDeFabrication(id, TypeDElementDeFabrication.ORDRE_DE_FABRICATION, OF_2026_000001, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fiche");
  }

  @Test
  void shouldBuildOrdreDeFabricationFromStepBuilder() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();

    ElementDeFabrication element = ElementDeFabrication.builder()
      .id(id)
      .type(TypeDElementDeFabrication.ORDRE_DE_FABRICATION)
      .nom(Nom.of(new Prefixe("OF"), new Annee(2026), 1))
      .titre("Assemblage carter")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_20_FEVRIER_2026);

    assertThat(element.id()).isEqualTo(id);
    assertThat(element.type()).isEqualTo(TypeDElementDeFabrication.ORDRE_DE_FABRICATION);
    assertThat(element.nom()).isEqualTo(OF_2026_000001);
    assertThat(element.titre()).isEqualTo(titreAssemblageCarter());
    assertThat(element.description()).isEqualTo(descriptionCarterEnFonte());
    assertThat(element.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(element.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }

  @Test
  void shouldBuildProduitFromStepBuilder() {
    ElementDeFabrication element = ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(Nom.of(new Prefixe("PRD"), new Annee(2026), 1))
      .titre("Carter moteur")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(element.type()).isEqualTo(TypeDElementDeFabrication.PRODUIT);
    assertThat(element.nom()).isEqualTo(PRD_2026_000001);
    assertThat(element.fiche()).isEqualTo(ficheCarterMoteur());
  }

  @Test
  void shouldReviseElementDeFabrication() {
    ElementDeFabrication element = elementDeFabricationAssemblageCarter();

    ElementDeFabrication revise = element.revise(titreAssemblageCarterRevise(), descriptionCarterEnFonte(), LE_20_FEVRIER_2026);

    assertThat(revise.id()).isEqualTo(element.id());
    assertThat(revise.type()).isEqualTo(element.type());
    assertThat(revise.nom()).isEqualTo(element.nom());
    assertThat(revise.titre()).isEqualTo(titreAssemblageCarterRevise());
    assertThat(revise.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(revise.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }
}

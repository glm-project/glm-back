package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationTest {

  @Test
  void shouldNotBuildWithoutId() {
    Fiche fiche = fiche1015();

    assertThatThrownBy(() -> new ElementDeFabrication(null, TypeDElementDeFabrication.ORDRE_DE_FABRICATION, OF_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutType() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Fiche fiche = fiche1015();

    assertThatThrownBy(() -> new ElementDeFabrication(id, null, OF_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Fiche fiche = fiche1015();

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
      .reference("1015")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_20_FEVRIER_2026);

    assertThat(element.id()).isEqualTo(id);
    assertThat(element.type()).isEqualTo(TypeDElementDeFabrication.ORDRE_DE_FABRICATION);
    assertThat(element.nom()).isEqualTo(OF_2026_000001);
    assertThat(element.reference()).contains(reference1015());
    assertThat(element.description()).contains(descriptionCarterEnFonte());
    assertThat(element.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(element.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }

  @Test
  void shouldBuildProduitFromStepBuilder() {
    ElementDeFabrication element = ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(Nom.of(new Prefixe("PRD"), new Annee(2026), 1))
      .reference("2456")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(element.type()).isEqualTo(TypeDElementDeFabrication.PRODUIT);
    assertThat(element.nom()).isEqualTo(PRD_2026_000001);
    assertThat(element.fiche()).isEqualTo(fiche2456());
  }

  @Test
  void shouldBuildProduitWithoutReferenceNorDescription() {
    ElementDeFabrication element = elementDeFabricationProduitSansReference();

    assertThat(element.reference()).isEmpty();
    assertThat(element.description()).isEmpty();
    assertThat(element.nom()).isEqualTo(PRD_2026_000001);
  }

  @Test
  void shouldReviseElementDeFabrication() {
    ElementDeFabrication element = elementDeFabricationOrdre1015();

    ElementDeFabrication revise = element.revise(Optional.of(reference1017()), Optional.of(descriptionCarterEnFonte()), LE_20_FEVRIER_2026);

    assertThat(revise.id()).isEqualTo(element.id());
    assertThat(revise.type()).isEqualTo(element.type());
    assertThat(revise.nom()).isEqualTo(element.nom());
    assertThat(revise.reference()).contains(reference1017());
    assertThat(revise.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(revise.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }
}

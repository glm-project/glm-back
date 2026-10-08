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

    assertThatThrownBy(() -> new ElementDeFabrication(null, CATEGORIE_OF, OF_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutType() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Fiche fiche = fiche1015();

    assertThatThrownBy(() -> new ElementDeFabrication(id, null, OF_2026_000001, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    Fiche fiche = fiche1015();

    assertThatThrownBy(() -> new ElementDeFabrication(id, CATEGORIE_OF, null, fiche))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutFiche() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();

    assertThatThrownBy(() -> new ElementDeFabrication(id, CATEGORIE_OF, OF_2026_000001, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fiche");
  }

  @Test
  void shouldBuildOrdreDeFabricationFromStepBuilder() {
    ElementDeFabricationId id = ElementDeFabricationId.newId();

    ElementDeFabrication element = ElementDeFabrication.builder()
      .id(id)
      .categorie(CATEGORIE_OF)
      .nom(Nom.of(new Categorie("OF"), new Annee(2026), 1))
      .reference("1015")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_20_FEVRIER_2026);

    assertThat(element.id()).isEqualTo(id);
    assertThat(element.categorie()).isEqualTo(CATEGORIE_OF);
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
      .categorie(CATEGORIE_MOULE)
      .nom(Nom.of(CATEGORIE_MOULE, new Annee(2026), 1))
      .reference("2456")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(element.categorie()).isEqualTo(CATEGORIE_MOULE);
    assertThat(element.nom()).isEqualTo(MOULE_2026_000001);
    assertThat(element.fiche()).isEqualTo(fiche2456());
  }

  @Test
  void shouldBuildProduitWithoutReferenceNorDescription() {
    ElementDeFabrication element = elementDeFabricationMouleSansReference();

    assertThat(element.reference()).isEmpty();
    assertThat(element.description()).isEmpty();
    assertThat(element.nom()).isEqualTo(MOULE_2026_000001);
  }

  @Test
  void shouldReviseElementDeFabrication() {
    ElementDeFabrication element = elementDeFabricationOrdre1015();

    ElementDeFabrication revise = element.revise(Optional.of(reference1017()), Optional.of(descriptionCarterEnFonte()), LE_20_FEVRIER_2026);

    assertThat(revise.id()).isEqualTo(element.id());
    assertThat(revise.categorie()).isEqualTo(element.categorie());
    assertThat(revise.nom()).isEqualTo(element.nom());
    assertThat(revise.reference()).contains(reference1017());
    assertThat(revise.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(revise.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }
}

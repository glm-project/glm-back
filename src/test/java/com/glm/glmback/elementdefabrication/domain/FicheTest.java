package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class FicheTest {

  @Test
  void shouldNotBuildWithoutReference() {
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new Fiche(null, description, LE_15_JANVIER_2026, LE_20_FEVRIER_2026))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    Optional<Reference> reference = Optional.of(reference1015());

    assertThatThrownBy(() -> new Fiche(reference, null, LE_15_JANVIER_2026, LE_20_FEVRIER_2026))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldNotBuildWithoutDateDeCreation() {
    Optional<Reference> reference = Optional.of(reference1015());
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new Fiche(reference, description, null, LE_20_FEVRIER_2026))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDeCreation");
  }

  @Test
  void shouldNotBuildWithoutDateDeModification() {
    Optional<Reference> reference = Optional.of(reference1015());
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new Fiche(reference, description, LE_15_JANVIER_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDeModification");
  }

  @Test
  void shouldNotBuildWithDateDeModificationBeforeDateDeCreation() {
    Optional<Reference> reference = Optional.of(reference1015());
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> new Fiche(reference, description, LE_20_FEVRIER_2026, LE_15_JANVIER_2026))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("dateDeModification");
  }

  @Test
  void shouldBuildNeverModifiedFiche() {
    Fiche fiche = Fiche.builder()
      .reference("1015")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(fiche.reference()).contains(reference1015());
    assertThat(fiche.description()).contains(descriptionCarterEnFonte());
    assertThat(fiche.dateDeModification()).isEqualTo(fiche.dateDeCreation());
  }

  @Test
  void shouldBuildFicheWithoutReferenceNorDescription() {
    Fiche fiche = Fiche.builder()
      .reference(null)
      .description(null)
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(fiche.reference()).isEmpty();
    assertThat(fiche.description()).isEmpty();
  }

  @Test
  void shouldReviseFiche() {
    Fiche revisee = fiche1015().revise(Optional.of(reference1017()), Optional.of(descriptionCarterEnFonte()), LE_20_FEVRIER_2026);

    assertThat(revisee.reference()).contains(reference1017());
    assertThat(revisee.description()).contains(descriptionCarterEnFonte());
    assertThat(revisee.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(revisee.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }

  @Test
  void shouldReviseFicheWithoutReferenceNorDescription() {
    Fiche revisee = fiche1015().revise(Optional.empty(), Optional.empty(), LE_20_FEVRIER_2026);

    assertThat(revisee.reference()).isEmpty();
    assertThat(revisee.description()).isEmpty();
    assertThat(revisee.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
  }

  @Test
  void shouldNotReviseBeforeDateDeCreation() {
    Fiche fiche = fiche1015();
    Optional<Reference> reference = Optional.of(reference1017());
    Optional<Description> description = Optional.of(descriptionCarterEnFonte());

    assertThatThrownBy(() -> fiche.revise(reference, description, LE_15_JANVIER_2026.minusSeconds(1)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("dateDeModification");
  }
}

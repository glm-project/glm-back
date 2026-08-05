package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import org.junit.jupiter.api.Test;

@UnitTest
class FicheTest {

  @Test
  void shouldNotBuildWithoutTitre() {
    assertThatThrownBy(() -> new Fiche(null, descriptionCarterEnFonte(), LE_15_JANVIER_2026, LE_20_FEVRIER_2026))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("titre");
  }

  @Test
  void shouldNotBuildWithoutDescription() {
    assertThatThrownBy(() -> new Fiche(titreAssemblageCarter(), null, LE_15_JANVIER_2026, LE_20_FEVRIER_2026))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldNotBuildWithoutDateDeCreation() {
    assertThatThrownBy(() -> new Fiche(titreAssemblageCarter(), descriptionCarterEnFonte(), null, LE_20_FEVRIER_2026))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDeCreation");
  }

  @Test
  void shouldNotBuildWithoutDateDeModification() {
    assertThatThrownBy(() -> new Fiche(titreAssemblageCarter(), descriptionCarterEnFonte(), LE_15_JANVIER_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dateDeModification");
  }

  @Test
  void shouldNotBuildWithDateDeModificationBeforeDateDeCreation() {
    assertThatThrownBy(() -> new Fiche(titreAssemblageCarter(), descriptionCarterEnFonte(), LE_20_FEVRIER_2026, LE_15_JANVIER_2026))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("dateDeModification");
  }

  @Test
  void shouldBuildNeverModifiedFiche() {
    Fiche fiche = Fiche.builder()
      .titre(titreAssemblageCarter().value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);

    assertThat(fiche.titre()).isEqualTo(titreAssemblageCarter());
    assertThat(fiche.description()).isEqualTo(descriptionCarterEnFonte());
    assertThat(fiche.dateDeModification()).isEqualTo(fiche.dateDeCreation());
  }

  @Test
  void shouldReviseFiche() {
    Fiche revisee = ficheAssemblageCarter().revise(titreAssemblageCarterRevise(), descriptionCarterEnFonte(), LE_20_FEVRIER_2026);

    assertThat(revisee.titre()).isEqualTo(titreAssemblageCarterRevise());
    assertThat(revisee.description()).isEqualTo(descriptionCarterEnFonte());
    assertThat(revisee.dateDeCreation()).isEqualTo(LE_15_JANVIER_2026);
    assertThat(revisee.dateDeModification()).isEqualTo(LE_20_FEVRIER_2026);
  }

  @Test
  void shouldNotReviseBeforeDateDeCreation() {
    Fiche fiche = ficheAssemblageCarter();
    Titre titre = titreAssemblageCarterRevise();
    Description description = descriptionCarterEnFonte();

    assertThatThrownBy(() -> fiche.revise(titre, description, LE_15_JANVIER_2026.minusSeconds(1)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("dateDeModification");
  }
}

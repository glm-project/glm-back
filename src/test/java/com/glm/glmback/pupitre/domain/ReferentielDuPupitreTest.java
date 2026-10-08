package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class ReferentielDuPupitreTest {

  private static final MaximumActivityDuration DUREE_MAXIMALE = MaximumActivityDuration.standard();

  @Test
  void shouldNotBuildWithoutDateDeGeneration() {
    assertThatThrownBy(() -> new ReferentielDuPupitre(null, List.of(), List.of(), List.of(), DUREE_MAXIMALE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de generation");
  }

  @Test
  void shouldNotBuildWithoutOperateurs() {
    assertThatThrownBy(() -> new ReferentielDuPupitre(LE_10_MAI_2026_A_8H, null, List.of(), List.of(), DUREE_MAXIMALE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateurs");
  }

  @Test
  void shouldNotBuildWithoutSuivis() {
    assertThatThrownBy(() -> new ReferentielDuPupitre(LE_10_MAI_2026_A_8H, List.of(), null, List.of(), DUREE_MAXIMALE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("suivis");
  }

  @Test
  void shouldNotBuildWithoutCategories() {
    assertThatThrownBy(() -> new ReferentielDuPupitre(LE_10_MAI_2026_A_8H, List.of(), List.of(), null, DUREE_MAXIMALE))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categories");
  }

  @Test
  void shouldNotBuildWithoutDureeMaximaleDActivite() {
    assertThatThrownBy(() -> new ReferentielDuPupitre(LE_10_MAI_2026_A_8H, List.of(), List.of(), List.of(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("duree maximale d'activite");
  }

  /**
   * La date est la version : elle dit de quand date ce que l'ecran d'atelier affiche.
   */
  @Test
  void shouldPorterLaDateDeSonInstantane() {
    ReferentielDuPupitre referentiel = new ReferentielDuPupitre(
      LE_10_MAI_2026_A_8H,
      List.of(OPERATEUR_DUPONT),
      List.of(suiviOf42Vierge()),
      List.of(CATEGORIE_MOULE, CATEGORIE_OF),
      DUREE_MAXIMALE
    );

    assertThat(referentiel.genereLe()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(referentiel.operateurs()).containsExactly(OPERATEUR_DUPONT);
    assertThat(referentiel.suivis()).containsExactly(suiviOf42Vierge());
    assertThat(referentiel.categories()).containsExactly(CATEGORIE_MOULE, CATEGORIE_OF);
    assertThat(referentiel.dureeMaximaleDActivite()).isEqualTo(DUREE_MAXIMALE);
  }
}

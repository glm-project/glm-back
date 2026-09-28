package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteTest {

  @Test
  void shouldNotBuildWithoutElement() {
    assertThatThrownBy(() ->
      Activite.builder().element(null).poste(Optional.empty()).nature(Optional.empty()).categorie(CategorieDActivite.TRAVAIL)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("element");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() ->
      Activite.builder().element(ELEMENT_ID_CARTER).poste(null).nature(Optional.empty()).categorie(CategorieDActivite.TRAVAIL)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() ->
      Activite.builder().element(ELEMENT_ID_CARTER).poste(Optional.empty()).nature(null).categorie(CategorieDActivite.TRAVAIL)
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() -> Activite.builder().element(ELEMENT_ID_CARTER).poste(Optional.empty()).nature(Optional.empty()).categorie(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldPorterSonElementSonPosteSaNatureEtSaCategorie() {
    Activite activite = Activite.builder()
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.of(POSTE_ID_DMU_50))
      .nature(Optional.of(NATURE_FRAISAGE))
      .categorie(CategorieDActivite.NON_CONFORMITE);

    assertThat(activite.element()).isEqualTo(ELEMENT_ID_CARTER);
    assertThat(activite.poste()).contains(POSTE_ID_DMU_50);
    assertThat(activite.nature()).contains(NATURE_FRAISAGE);
    assertThat(activite.categorie()).isEqualTo(CategorieDActivite.NON_CONFORMITE);
  }
}

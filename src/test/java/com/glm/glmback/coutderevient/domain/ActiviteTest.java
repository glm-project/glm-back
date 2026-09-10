package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteTest {

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() ->
      new Activite(
        null,
        Optional.of(POSTE_ID_FRAISEUSE),
        Optional.of(NATURE_FRAISAGE),
        Optional.of(COUT_HORAIRE_DE_45_EUROS),
        Optional.of(TAUX_HORAIRE_DE_20_EUROS),
        CategorieDActivite.TRAVAIL
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() ->
      new Activite(
        OPERATEUR_ID_DUPONT,
        null,
        Optional.of(NATURE_FRAISAGE),
        Optional.of(COUT_HORAIRE_DE_45_EUROS),
        Optional.of(TAUX_HORAIRE_DE_20_EUROS),
        CategorieDActivite.TRAVAIL
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() ->
      new Activite(
        OPERATEUR_ID_DUPONT,
        Optional.of(POSTE_ID_FRAISEUSE),
        null,
        Optional.of(COUT_HORAIRE_DE_45_EUROS),
        Optional.of(TAUX_HORAIRE_DE_20_EUROS),
        CategorieDActivite.TRAVAIL
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l");
  }

  @Test
  void shouldNotBuildWithoutCoutHoraire() {
    assertThatThrownBy(() ->
      new Activite(
        OPERATEUR_ID_DUPONT,
        Optional.of(POSTE_ID_FRAISEUSE),
        Optional.of(NATURE_FRAISAGE),
        null,
        Optional.of(TAUX_HORAIRE_DE_20_EUROS),
        CategorieDActivite.TRAVAIL
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldNotBuildWithoutTauxHoraire() {
    assertThatThrownBy(() ->
      new Activite(
        OPERATEUR_ID_DUPONT,
        Optional.of(POSTE_ID_FRAISEUSE),
        Optional.of(NATURE_FRAISAGE),
        Optional.of(COUT_HORAIRE_DE_45_EUROS),
        null,
        CategorieDActivite.TRAVAIL
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() ->
      new Activite(
        OPERATEUR_ID_DUPONT,
        Optional.of(POSTE_ID_FRAISEUSE),
        Optional.of(NATURE_FRAISAGE),
        Optional.of(COUT_HORAIRE_DE_45_EUROS),
        Optional.of(TAUX_HORAIRE_DE_20_EUROS),
        null
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldBuildFromBuilder() {
    Activite activite = Activite.builder()
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .categorie(CategorieDActivite.TRAVAIL);

    assertThat(activite.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
  }
}

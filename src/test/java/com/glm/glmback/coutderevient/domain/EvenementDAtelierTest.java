package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class EvenementDAtelierTest {

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() ->
      new EvenementDAtelier(
        null,
        OPERATEUR_ID_DUPONT,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        LE_11_MAI_A_9H
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() ->
      new EvenementDAtelier(
        TypeDEvenementDAtelier.DEBUT,
        null,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        LE_11_MAI_A_9H
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() ->
      new EvenementDAtelier(
        TypeDEvenementDAtelier.DEBUT,
        OPERATEUR_ID_DUPONT,
        null,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        LE_11_MAI_A_9H
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() ->
      new EvenementDAtelier(
        TypeDEvenementDAtelier.DEBUT,
        OPERATEUR_ID_DUPONT,
        Optional.empty(),
        null,
        Optional.empty(),
        Optional.empty(),
        LE_11_MAI_A_9H
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l");
  }

  @Test
  void shouldNotBuildWithoutCoutHoraire() {
    assertThatThrownBy(() ->
      new EvenementDAtelier(
        TypeDEvenementDAtelier.DEBUT,
        OPERATEUR_ID_DUPONT,
        Optional.empty(),
        Optional.empty(),
        null,
        Optional.empty(),
        LE_11_MAI_A_9H
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldNotBuildWithoutTauxHoraire() {
    assertThatThrownBy(() ->
      new EvenementDAtelier(
        TypeDEvenementDAtelier.DEBUT,
        OPERATEUR_ID_DUPONT,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        null,
        LE_11_MAI_A_9H
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldNotBuildWithoutDateDeSurvenue() {
    assertThatThrownBy(() ->
      new EvenementDAtelier(
        TypeDEvenementDAtelier.DEBUT,
        OPERATEUR_ID_DUPONT,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        null
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de survenue");
  }

  @Test
  void shouldExposeCleDActivite() {
    EvenementDAtelier evenement = EvenementDAtelier.builder()
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .dateDeSurvenue(LE_11_MAI_A_9H);

    assertThat(evenement.cle()).isEqualTo(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE)));
  }
}

package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SyntheseDesHeuresTest {

  private static final JourDeSynthese LUNDI_8H = JourDeSynthese.builder()
    .jour(LocalDate.of(2026, 5, 11))
    .pointages(List.of())
    .dureeOperationnelle(Duration.ofHours(10));
  private static final JourDeSynthese MARDI_0H = JourDeSynthese.builder()
    .jour(LocalDate.of(2026, 5, 12))
    .pointages(List.of())
    .dureeOperationnelle(Duration.ZERO);
  private static final JourDeSynthese MERCREDI_1H = JourDeSynthese.builder()
    .jour(LocalDate.of(2026, 5, 13))
    .pointages(List.of())
    .dureeOperationnelle(Duration.ofHours(1));

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new SyntheseDesHeures(null, SEMAINE_20_DE_2026, List.of(), List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutSemaine() {
    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, null, List.of(), List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("semaine");
  }

  @Test
  void shouldNotBuildWithoutJours() {
    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldNotBuildWithNullJour() {
    List<JourDeSynthese> jours = Arrays.asList(LUNDI_8H, null);

    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, jours, List.of()))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldPorterLOperateurSaSemaineEtSesJours() {
    SyntheseDesHeures synthese = new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, List.of(LUNDI_8H), List.of());

    assertThat(synthese.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(synthese.semaine()).isEqualTo(SEMAINE_20_DE_2026);
    assertThat(synthese.jours()).containsExactly(LUNDI_8H);
  }

  @Test
  void shouldNotBuildWithoutElements() {
    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, List.of(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("elements");
  }

  @Test
  void shouldNotBuildWithNullElement() {
    List<ElementDeLaSynthese> elements = Arrays.asList(
      new ElementDeLaSynthese(ELEMENT_ENGAGE_CARTER, Optional.empty(), Optional.empty(), Duration.ZERO, Duration.ZERO, List.of()),
      null
    );

    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, List.of(), elements))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("elements");
  }

  @Test
  void shouldSommerLeTempsOperationnelDeChaqueJour() {
    SyntheseDesHeures synthese = SyntheseDesHeures.builder()
      .operateur(OPERATEUR_CONNU_DUPONT)
      .semaine(SEMAINE_20_DE_2026)
      .jours(List.of(LUNDI_8H, MARDI_0H, MERCREDI_1H))
      .elements(List.of());

    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(Duration.ofHours(11));
  }
}

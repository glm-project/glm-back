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
import org.junit.jupiter.api.Test;

@UnitTest
class SyntheseDesHeuresTest {

  private static final JourDeSynthese LUNDI_8H = new JourDeSynthese(
    LocalDate.of(2026, 5, 11),
    List.of(new Pointage(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), true)),
    Duration.ofHours(8)
  );
  private static final JourDeSynthese MARDI_SANS_PRESENCE = new JourDeSynthese(LocalDate.of(2026, 5, 12), List.of(), Duration.ZERO);
  private static final JourDeSynthese MERCREDI_AVEC_ANOMALIE = new JourDeSynthese(
    LocalDate.of(2026, 5, 13),
    List.of(new Pointage(pauseA(LE_LUNDI_11_MAI_2026_A_12H), false)),
    Duration.ZERO
  );

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new SyntheseDesHeures(null, SEMAINE_20_DE_2026, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutSemaine() {
    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("semaine");
  }

  @Test
  void shouldNotBuildWithoutJours() {
    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldNotBuildWithNullJour() {
    List<JourDeSynthese> jours = Arrays.asList(LUNDI_8H, null);

    assertThatThrownBy(() -> new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, jours))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldPorterLOperateurSaSemaineEtSesJours() {
    SyntheseDesHeures synthese = new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, List.of(LUNDI_8H));

    assertThat(synthese.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(synthese.semaine()).isEqualTo(SEMAINE_20_DE_2026);
    assertThat(synthese.jours()).containsExactly(LUNDI_8H);
  }

  @Test
  void shouldSommerLaDureeDeChaqueJourPourLaDureeTotale() {
    SyntheseDesHeures synthese = new SyntheseDesHeures(
      OPERATEUR_CONNU_DUPONT,
      SEMAINE_20_DE_2026,
      List.of(LUNDI_8H, MARDI_SANS_PRESENCE)
    );

    assertThat(synthese.dureeTotale()).isEqualTo(Duration.ofHours(8));
  }

  @Test
  void shouldNotSignalerDAnomalieQuandAucunJourNEnPorte() {
    SyntheseDesHeures synthese = new SyntheseDesHeures(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, List.of(LUNDI_8H, MARDI_SANS_PRESENCE));

    assertThat(synthese.aUneAnomalie()).isFalse();
  }

  @Test
  void shouldSignalerUneAnomalieDesQuUnJourEnPorteUne() {
    SyntheseDesHeures synthese = new SyntheseDesHeures(
      OPERATEUR_CONNU_DUPONT,
      SEMAINE_20_DE_2026,
      List.of(LUNDI_8H, MERCREDI_AVEC_ANOMALIE)
    );

    assertThat(synthese.aUneAnomalie()).isTrue();
  }
}

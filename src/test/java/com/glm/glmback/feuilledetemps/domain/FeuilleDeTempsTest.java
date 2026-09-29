package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class FeuilleDeTempsTest {

  private static final JourDeLaSemaine LUNDI_SANS_ACTIVITE = new JourDeLaSemaine(LUNDI_11_MAI_2026, List.of());

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new FeuilleDeTemps(null, SEMAINE_20_DE_2026, LE_MARDI_12_MAI_2026_A_10H, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutSemaine() {
    assertThatThrownBy(() -> new FeuilleDeTemps(OPERATEUR_CONNU_DUPONT, null, LE_MARDI_12_MAI_2026_A_10H, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("semaine");
  }

  @Test
  void shouldNotBuildWithoutEvaluation() {
    assertThatThrownBy(() ->
      FeuilleDeTemps.builder().operateur(OPERATEUR_CONNU_DUPONT).semaine(SEMAINE_20_DE_2026).evaluation(null).jours(List.of())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evaluation");
  }

  @Test
  void shouldNotBuildWithoutJours() {
    assertThatThrownBy(() -> new FeuilleDeTemps(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, LE_MARDI_12_MAI_2026_A_10H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldNotBuildWithNullJour() {
    List<JourDeLaSemaine> jours = Arrays.asList(LUNDI_SANS_ACTIVITE, null);

    assertThatThrownBy(() -> new FeuilleDeTemps(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, LE_MARDI_12_MAI_2026_A_10H, jours))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldPorterLOperateurResoluSaSemaineEtSesJours() {
    FeuilleDeTemps feuille = new FeuilleDeTemps(
      OPERATEUR_CONNU_DUPONT,
      SEMAINE_20_DE_2026,
      LE_MARDI_12_MAI_2026_A_10H,
      List.of(LUNDI_SANS_ACTIVITE)
    );

    assertThat(feuille.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(feuille.semaine()).isEqualTo(SEMAINE_20_DE_2026);
    assertThat(feuille.jours()).containsExactly(LUNDI_SANS_ACTIVITE);
  }
}

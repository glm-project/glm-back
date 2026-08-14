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

  private static final JourDeLaSemaine LUNDI_SANS_PRESENCE = new JourDeLaSemaine(LUNDI_11_MAI_2026, List.of());

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new FeuilleDeTemps(null, SEMAINE_20_DE_2026, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutSemaine() {
    assertThatThrownBy(() -> new FeuilleDeTemps(OPERATEUR_CONNU_DUPONT, null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("semaine");
  }

  @Test
  void shouldNotBuildWithoutJours() {
    assertThatThrownBy(() -> new FeuilleDeTemps(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldNotBuildWithNullJour() {
    List<JourDeLaSemaine> jours = Arrays.asList(LUNDI_SANS_PRESENCE, null);

    assertThatThrownBy(() -> new FeuilleDeTemps(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, jours))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("jours");
  }

  @Test
  void shouldPorterLOperateurResoluSaSemaineEtSesJours() {
    FeuilleDeTemps feuille = new FeuilleDeTemps(OPERATEUR_CONNU_DUPONT, SEMAINE_20_DE_2026, List.of(LUNDI_SANS_PRESENCE));

    assertThat(feuille.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(feuille.semaine()).isEqualTo(SEMAINE_20_DE_2026);
    assertThat(feuille.jours()).containsExactly(LUNDI_SANS_PRESENCE);
  }
}

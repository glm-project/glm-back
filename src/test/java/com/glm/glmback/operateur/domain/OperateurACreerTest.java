package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurACreerTest {

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new OperateurACreer(null, PRENOM_JEAN, Optional.empty(), TAUX_HORAIRE_22, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutPrenom() {
    assertThatThrownBy(() -> new OperateurACreer(NOM_DUPONT, null, Optional.empty(), TAUX_HORAIRE_22, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithoutIdentifiant() {
    assertThatThrownBy(() -> new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, null, TAUX_HORAIRE_22, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("identifiant");
  }

  @Test
  void shouldNotBuildWithoutTauxHoraire() {
    assertThatThrownBy(() -> new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, Optional.empty(), null, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    assertThatThrownBy(() -> new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, Optional.empty(), TAUX_HORAIRE_22, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    OperateurACreer aCreer = new OperateurACreer("Dupont", "Jean", "049", new BigDecimal("22.00"), habilitationDeTournage());

    assertThat(aCreer.nom()).isEqualTo(NOM_DUPONT);
    assertThat(aCreer.prenom()).isEqualTo(PRENOM_JEAN);
    assertThat(aCreer.identifiant()).contains(IDENTIFIANT_049);
    assertThat(aCreer.tauxHoraire()).isEqualTo(TAUX_HORAIRE_22);
    assertThat(aCreer.postes()).containsExactly(ID_TOUR_1);
  }

  @Test
  void shouldAcceptBlankIdentifiantAsAbsent() {
    OperateurACreer aCreer = new OperateurACreer("Dupont", "Jean", " ", new BigDecimal("22.00"), Set.of());

    assertThat(aCreer.identifiant()).isEmpty();
  }

  @Test
  void shouldNotBuildWithoutRawTauxHoraire() {
    assertThatThrownBy(() -> new OperateurACreer("Dupont", "Jean", "049", null, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("taux horaire");
  }
}

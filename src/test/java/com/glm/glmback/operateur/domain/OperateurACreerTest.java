package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurACreerTest {

  @Test
  void shouldNotBuildWithoutNom() {
    assertThatThrownBy(() -> new OperateurACreer(null, PRENOM_JEAN, Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutPrenom() {
    assertThatThrownBy(() -> new OperateurACreer(NOM_DUPONT, null, Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithoutMatricule() {
    assertThatThrownBy(() -> new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, null, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("matricule");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    assertThatThrownBy(() -> new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    OperateurACreer aCreer = new OperateurACreer("Dupont", "Jean", "049", habilitationDeTournage());

    assertThat(aCreer.nom()).isEqualTo(NOM_DUPONT);
    assertThat(aCreer.prenom()).isEqualTo(PRENOM_JEAN);
    assertThat(aCreer.matricule()).contains(MATRICULE_049);
    assertThat(aCreer.postes()).containsExactly(ID_TOUR_1);
  }

  @Test
  void shouldAcceptBlankMatriculeAsAbsent() {
    OperateurACreer aCreer = new OperateurACreer("Dupont", "Jean", " ", Set.of());

    assertThat(aCreer.matricule()).isEmpty();
  }
}

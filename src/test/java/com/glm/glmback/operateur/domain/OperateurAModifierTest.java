package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurAModifierTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new OperateurAModifier(null, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new OperateurAModifier(id, null, PRENOM_JEAN, Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutPrenom() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new OperateurAModifier(id, NOM_DUPONT, null, Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithoutMatricule() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new OperateurAModifier(id, NOM_DUPONT, PRENOM_JEAN, null, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("matricule");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new OperateurAModifier(id, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldWrapRawValuesInValueObjects() {
    OperateurId id = OperateurId.newId();

    OperateurAModifier aModifier = new OperateurAModifier(id, "Dupont", "Jean", "049", habilitationDeTournage());

    assertThat(aModifier.id()).isEqualTo(id);
    assertThat(aModifier.nom()).isEqualTo(NOM_DUPONT);
    assertThat(aModifier.prenom()).isEqualTo(PRENOM_JEAN);
    assertThat(aModifier.matricule()).contains(MATRICULE_049);
    assertThat(aModifier.postes()).containsExactly(ID_TOUR_1);
  }

  @Test
  void shouldRemoveMatriculeWhenBlank() {
    OperateurId id = OperateurId.newId();

    OperateurAModifier aModifier = new OperateurAModifier(id, "Dupont", "Jean", " ", Set.of());

    assertThat(aModifier.matricule()).isEmpty();
  }
}

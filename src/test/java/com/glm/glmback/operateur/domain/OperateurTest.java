package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new Operateur(null, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutNom() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new Operateur(id, null, PRENOM_JEAN, Optional.empty(), Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithoutPrenom() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new Operateur(id, NOM_DUPONT, null, Optional.empty(), Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithoutIdentifiant() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new Operateur(id, NOM_DUPONT, PRENOM_JEAN, null, Optional.empty(), Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("identifiant");
  }

  @Test
  void shouldNotBuildWithoutTauxHoraire() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new Operateur(id, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), null, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    OperateurId id = OperateurId.newId();

    assertThatThrownBy(() -> new Operateur(id, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldNotBuildWithNullPoste() {
    OperateurId id = OperateurId.newId();
    Set<PosteHabilitableId> avecNull = new HashSet<>();
    avecNull.add(null);

    assertThatThrownBy(() -> new Operateur(id, NOM_DUPONT, PRENOM_JEAN, Optional.empty(), Optional.empty(), avecNull))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("postes");
  }

  @Test
  void shouldBuildOperateurFromRawValues() {
    OperateurId id = OperateurId.newId();

    Operateur operateur = Operateur.builder()
      .id(id)
      .nom(NOM_DUPONT)
      .prenom(PRENOM_JEAN)
      .identifiant("049")
      .tauxHoraire(new BigDecimal("22.00"))
      .postes(habilitationsDeSoudureEtDeTournage());

    assertThat(operateur.id()).isEqualTo(id);
    assertThat(operateur.nom()).isEqualTo(NOM_DUPONT);
    assertThat(operateur.prenom()).isEqualTo(PRENOM_JEAN);
    assertThat(operateur.identifiant()).contains(IDENTIFIANT_049);
    assertThat(operateur.tauxHoraire()).contains(TAUX_HORAIRE_22);
    assertThat(operateur.postes()).containsExactlyInAnyOrder(ID_TOUR_1, ID_POSTE_DE_SOUDURE);
  }

  @Test
  void shouldBuildOperateurWithoutIdentifiantNorPoste() {
    Operateur operateur = Operateur.builder()
      .id(OperateurId.newId())
      .nom(NOM_MARTIN)
      .prenom(PRENOM_SOPHIE)
      .identifiant(null)
      .tauxHoraire(null)
      .postes(Set.of());

    assertThat(operateur.identifiant()).isEmpty();
    assertThat(operateur.tauxHoraire()).isEmpty();
    assertThat(operateur.postes()).isEmpty();
  }

  @Test
  void shouldNotLetCallerChangePostesAfterBuild() {
    Set<PosteHabilitableId> modifiable = new HashSet<>(habilitationDeTournage());
    Operateur operateur = new Operateur(OperateurId.newId(), NOM_DUPONT, PRENOM_JEAN, Optional.empty(), Optional.empty(), modifiable);

    modifiable.add(ID_POSTE_DE_SOUDURE);

    assertThat(operateur.postes()).containsExactly(ID_TOUR_1);
  }

  @Test
  void shouldKeepIdentityWhenRevising() {
    Operateur operateur = operateurDupont();

    Operateur revise = operateur.revise(
      NOM_MARTIN,
      PRENOM_SOPHIE,
      Optional.of(IDENTIFIANT_050),
      Optional.of(TAUX_HORAIRE_25),
      habilitationDeTournage()
    );

    assertThat(revise.id()).isEqualTo(operateur.id());
    assertThat(revise.nom()).isEqualTo(NOM_MARTIN);
    assertThat(revise.prenom()).isEqualTo(PRENOM_SOPHIE);
    assertThat(revise.identifiant()).contains(IDENTIFIANT_050);
    assertThat(revise.tauxHoraire()).contains(TAUX_HORAIRE_25);
    assertThat(revise.postes()).containsExactly(ID_TOUR_1);
  }
}

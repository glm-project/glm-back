package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class ProfilDOperateurTest {

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new ProfilDOperateur(null, List.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutPostes() {
    Operateur operateur = operateurDupont();

    assertThatThrownBy(() -> new ProfilDOperateur(operateur, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("postes");
  }

  /**
   * Le test qui porte le lot : soudeur et tourneur sans que personne ne l'ait saisi.
   */
  @Test
  void shouldDeduceMetiersFromHabilitations() {
    ProfilDOperateur profil = new ProfilDOperateur(
      operateurDupont(),
      List.of(POSTE_HABILITABLE_TOUR_1, POSTE_HABILITABLE_POSTE_DE_SOUDURE)
    );

    assertThat(profil.natures()).containsExactly(NATURE_SOUDAGE, NATURE_TOURNAGE);
  }

  @Test
  void shouldSortPostesByLibelleToMakeReadsRepeatable() {
    ProfilDOperateur profil = new ProfilDOperateur(
      operateurDupont(),
      List.of(POSTE_HABILITABLE_TOUR_1, POSTE_HABILITABLE_POSTE_DE_SOUDURE)
    );

    assertThat(profil.postes()).containsExactly(POSTE_HABILITABLE_POSTE_DE_SOUDURE, POSTE_HABILITABLE_TOUR_1);
  }

  @Test
  void shouldDeduceNoMetierWithoutHabilitation() {
    ProfilDOperateur profil = new ProfilDOperateur(operateurMartinSansMatricule(), List.of());

    assertThat(profil.natures()).isEmpty();
  }

  @Test
  void shouldDeduceSingleMetierFromTwoPostesOfSameNature() {
    PosteHabilitable autreTour = new PosteHabilitable(ID_POSTE_DE_SOUDURE, new LibelleDePoste("Tour 2"), NATURE_TOURNAGE);

    ProfilDOperateur profil = new ProfilDOperateur(operateurDupont(), List.of(POSTE_HABILITABLE_TOUR_1, autreTour));

    assertThat(profil.natures()).containsExactly(NATURE_TOURNAGE);
  }
}

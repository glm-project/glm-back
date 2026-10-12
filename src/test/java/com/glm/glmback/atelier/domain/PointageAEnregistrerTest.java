package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class PointageAEnregistrerTest {

  @ParameterizedTest
  @EnumSource(value = TypeDEvenementDAtelier.class, names = { "DEBUT", "NON_CONFORMITE", "FIN" })
  void shouldPointerUnGeste(TypeDEvenementDAtelier type) {
    PointageAEnregistrer pointage = PointageAEnregistrer.builder()
      .suivi(SuiviDAtelierId.newId())
      .type(type)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT);

    assertThat(pointage.type()).isEqualTo(type);
  }

  /**
   * Une correction est un acte du gestionnaire sur une activite : le pupitre ne la pointe pas.
   */
  @ParameterizedTest
  @EnumSource(value = TypeDEvenementDAtelier.class, names = { "RETRAIT", "CHANGEMENT_DE_POSTE" })
  void shouldNePasPointerUneCorrection(TypeDEvenementDAtelier type) {
    var builder = PointageAEnregistrer.builder()
      .suivi(SuiviDAtelierId.newId())
      .type(type)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1));

    assertThatThrownBy(() -> builder.auteur(AUTEUR_DUPONT))
      .isExactlyInstanceOf(EvenementDAtelierIncoherentException.class)
      .hasMessageContaining(type.name())
      .hasMessageContaining("POINTAGE");
  }
}

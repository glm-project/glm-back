package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class IntervalleDActiviteTest {

  private static final Activite FRAISAGE_DE_DUPONT = Activite.builder()
    .operateur(OPERATEUR_ID_DUPONT)
    .poste(Optional.of(POSTE_ID_FRAISEUSE))
    .nature(Optional.of(NATURE_FRAISAGE))
    .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
    .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
    .categorie(CategorieDActivite.TRAVAIL);

  @Test
  void shouldNotBuildWithoutActivite() {
    assertThatThrownBy(() -> new IntervalleDActivite(null, new Plage(LE_11_MAI_A_9H, Optional.empty())))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activite");
  }

  @Test
  void shouldNotBuildWithoutPlage() {
    assertThatThrownBy(() -> new IntervalleDActivite(FRAISAGE_DE_DUPONT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("plage");
  }

  /**
   * Une pause de midi scinde le travail : c'est la reduction aux fenetres de presence, et non le journal de
   * l'element, qui produit ce second intervalle.
   */
  @Test
  void shouldReduceToFenetreDePresence() {
    IntervalleDActivite intervalle = new IntervalleDActivite(FRAISAGE_DE_DUPONT, new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_14H)));

    Optional<IntervalleDActivite> reduit = intervalle.reduitA(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_12H)));

    assertThat(reduit).contains(new IntervalleDActivite(FRAISAGE_DE_DUPONT, new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_12H))));
  }

  @Test
  void shouldNotReduceToDisjointFenetre() {
    IntervalleDActivite intervalle = new IntervalleDActivite(FRAISAGE_DE_DUPONT, new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_10H)));

    assertThat(intervalle.reduitA(new Plage(LE_11_MAI_A_13H, Optional.of(LE_11_MAI_A_17H)))).isEmpty();
  }

  @Test
  void shouldCloseOpenIntervalleAtInstant() {
    IntervalleDActivite intervalle = new IntervalleDActivite(FRAISAGE_DE_DUPONT, new Plage(LE_11_MAI_A_9H, Optional.empty()));

    assertThat(intervalle.ferme(LE_11_MAI_A_11H)).isEqualTo(
      new TrancheDActivite(FRAISAGE_DE_DUPONT, new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H))
    );
  }

  @Test
  void shouldExposeOperateurOfItsActivite() {
    IntervalleDActivite intervalle = new IntervalleDActivite(FRAISAGE_DE_DUPONT, new Plage(LE_11_MAI_A_9H, Optional.empty()));

    assertThat(intervalle.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
  }
}

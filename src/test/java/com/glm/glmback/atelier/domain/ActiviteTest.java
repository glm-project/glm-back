package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteTest {

  @Test
  void shouldNotBuildWithoutOuvrant() {
    assertThatThrownBy(() -> new Activite(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("ouvrant");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> new Activite(ouvrant, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotEndBeforeItsDebut() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    Optional<Instant> avantLeDebut = Optional.of(LE_10_MAI_2026_A_7H);

    assertThatThrownBy(() -> new Activite(ouvrant, avantLeDebut))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  /**
   * Une activite porte l'identite de son pointage ouvrant d'origine, et tient de lui sa cle, sa categorie et son
   * debut.
   */
  @Test
  void shouldTenirDeSonOuvrantSonIdentiteSaCleSaCategorieEtSonDebut() {
    EvenementDAtelier ouvrant = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    Activite activite = Activite.ouvertePar(ouvrant);

    assertThat(activite.id()).isEqualTo(ActiviteId.ouvertePar(ouvrant.id()));
    assertThat(activite.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
    assertThat(activite.categorie()).isEqualTo(CategorieDActivite.NON_CONFORMITE);
    assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(activite.fin()).isEmpty();
  }

  @Test
  void shouldGarderSonOuvrantUneFoisTerminee() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    Activite terminee = Activite.ouvertePar(ouvrant).termineeA(LE_10_MAI_2026_A_12H);

    assertThat(terminee.ouvrant()).isEqualTo(ouvrant);
    assertThat(terminee.fin()).contains(LE_10_MAI_2026_A_12H);
  }

  @Test
  void shouldSeLireEnIntervalleOuvertParSonPointage() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    IntervalleDActivite intervalle = Activite.ouvertePar(ouvrant).termineeA(LE_10_MAI_2026_A_12H).intervalle();

    assertThat(intervalle.evenement()).isEqualTo(ouvrant.id());
    assertThat(intervalle.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
    assertThat(intervalle.nature()).contains(NATURE_FRAISAGE);
    assertThat(intervalle.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
    assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_12H);
  }
}

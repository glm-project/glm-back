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
class IntervalleDActiviteTest {

  private static final EvenementDAtelierId EVENEMENT = EvenementDAtelierId.newId();

  @Test
  void shouldNotBuildWithoutEvenement() {
    assertThatThrownBy(() -> intervalle(null, LE_10_MAI_2026_A_8H, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evenement");
  }

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> intervalle(EVENEMENT, null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> intervalle(EVENEMENT, LE_10_MAI_2026_A_8H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() ->
      new IntervalleDActivite(
        EVENEMENT,
        OPERATEUR_ID_DUPONT,
        null,
        Optional.of(NATURE_FRAISAGE),
        CategorieDActivite.TRAVAIL,
        LE_10_MAI_2026_A_8H,
        Optional.empty()
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() ->
      new IntervalleDActivite(
        EVENEMENT,
        OPERATEUR_ID_DUPONT,
        Optional.of(POSTE_ID_FRAISEUSE_1),
        null,
        CategorieDActivite.TRAVAIL,
        LE_10_MAI_2026_A_8H,
        Optional.empty()
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() ->
      new IntervalleDActivite(
        EVENEMENT,
        null,
        Optional.of(POSTE_ID_FRAISEUSE_1),
        Optional.of(NATURE_FRAISAGE),
        CategorieDActivite.TRAVAIL,
        LE_10_MAI_2026_A_8H,
        Optional.empty()
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutCategorie() {
    assertThatThrownBy(() ->
      new IntervalleDActivite(
        EVENEMENT,
        OPERATEUR_ID_DUPONT,
        Optional.of(POSTE_ID_FRAISEUSE_1),
        Optional.of(NATURE_FRAISAGE),
        null,
        LE_10_MAI_2026_A_8H,
        Optional.empty()
      )
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("categorie");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> intervalle(EVENEMENT, LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_8H)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldBeOuvertWithoutFin() {
    IntervalleDActivite intervalle = intervalle(EVENEMENT, LE_10_MAI_2026_A_8H, Optional.empty());

    assertThat(intervalle.estOuvert()).isTrue();
    assertThat(intervalle.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
  }

  @Test
  void shouldNotBeOuvertWithFin() {
    assertThat(intervalleFerme().estOuvert()).isFalse();
  }

  @Test
  void shouldKeepBornes() {
    assertThat(intervalleFerme().debut()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(intervalleFerme().fin()).contains(LE_10_MAI_2026_A_12H);
    assertThat(intervalleFerme().categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
    assertThat(intervalleFerme().nature()).contains(NATURE_FRAISAGE);
  }

  @Test
  void shouldReduireLIntervalleALaFenetreDePresence() {
    FenetreDePresence matinee = new FenetreDePresence(LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_12H));

    IntervalleDActivite reduit = intervalleFerme().reduitA(matinee).orElseThrow();

    assertThat(reduit.debut()).isEqualTo(LE_10_MAI_2026_A_9H);
    assertThat(reduit.fin()).contains(LE_10_MAI_2026_A_12H);
    assertThat(reduit.evenement()).isEqualTo(EVENEMENT);
    assertThat(reduit.poste()).contains(POSTE_ID_FRAISEUSE_1);
  }

  @Test
  void shouldNotReduireLIntervalleAUneFenetreDisjointe() {
    FenetreDePresence apresMidi = new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H));

    assertThat(intervalleFerme().reduitA(apresMidi)).isEmpty();
  }

  private static IntervalleDActivite intervalleFerme() {
    return intervalle(EVENEMENT, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H));
  }

  private static IntervalleDActivite intervalle(EvenementDAtelierId evenement, Instant debut, Optional<Instant> fin) {
    return IntervalleDActivite.builder()
      .evenement(evenement)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .categorie(CategorieDActivite.TRAVAIL)
      .debut(debut)
      .fin(fin);
  }
}

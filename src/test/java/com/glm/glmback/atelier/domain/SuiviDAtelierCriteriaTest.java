package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDAtelierCriteriaTest {

  @Test
  void shouldNotBuildWithoutPeriode() {
    assertThatThrownBy(() -> new SuiviDAtelierCriteria(null, Set.of(), LE_10_MAI_2026_A_17H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("periode");
  }

  @Test
  void shouldNotBuildWithoutEvaluation() {
    assertThatThrownBy(() -> new SuiviDAtelierCriteria(Optional.empty(), Set.of(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evaluation");
  }

  @Test
  void shouldNotBuildWithoutEtats() {
    assertThatThrownBy(() -> new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), (Set<EtatDAtelier>) null, LE_10_MAI_2026_A_17H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("etats");
  }

  /**
   * L'ecran des operateurs : tous les elements actifs, sans notion de date.
   */
  @Test
  void shouldMatchSansAucunePeriode() {
    assertThat(new SuiviDAtelierCriteria(Optional.empty(), Set.of(), LE_10_MAI_2026_A_17H).matches(suiviDAtelierEngage())).isTrue();
  }

  @Test
  void shouldMatchAnyEtatWhenAucunEtatDemande() {
    assertThat(
      new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), Set.of(), LE_10_MAI_2026_A_17H).matches(suiviDAtelierEngage())
    ).isTrue();
  }

  @Test
  void shouldNotMatchSuiviEngageHorsDeLaPeriode() {
    Periode lendemain = new Periode(LE_11_MAI_2026_A_9H15, LE_11_MAI_2026_A_9H15);

    assertThat(new SuiviDAtelierCriteria(Optional.of(lendemain), Set.of(), LE_10_MAI_2026_A_17H).matches(suiviDAtelierEngage())).isFalse();
  }

  @Test
  void shouldMatchDemandedEtat() {
    assertThat(
      new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), Set.of(EtatDAtelier.EN_ATTENTE), LE_10_MAI_2026_A_17H).matches(
        suiviDAtelierEngage()
      )
    ).isTrue();
  }

  @Test
  void shouldNotMatchOtherEtat() {
    assertThat(
      new SuiviDAtelierCriteria(Optional.of(journeeDu10Mai2026()), Set.of(EtatDAtelier.EN_COURS), LE_10_MAI_2026_A_17H).matches(
        suiviDAtelierEngage()
      )
    ).isFalse();
  }

  /**
   * L'etat se juge a l'instant d'evaluation : l'element dont l'activite a atteint son echeance n'est plus en cours.
   */
  @Test
  void shouldJugerLEtatALInstantDEvaluation() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    Instant echeance = Instant.parse("2026-05-10T21:00:00Z");

    assertThat(
      new SuiviDAtelierCriteria(Optional.empty(), Set.of(EtatDAtelier.EN_COURS), echeance.minusSeconds(1)).matches(suivi)
    ).isTrue();
    assertThat(new SuiviDAtelierCriteria(Optional.empty(), Set.of(EtatDAtelier.EN_COURS), echeance).matches(suivi)).isFalse();
    assertThat(new SuiviDAtelierCriteria(Optional.empty(), Set.of(EtatDAtelier.INTERROMPU), echeance).matches(suivi)).isTrue();
  }

  @Test
  void shouldIncludeConflictsWithoutActivitiesOutsideSelectedStates() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debut)
      .enregistre(finDe(debut).a(LE_10_MAI_2026_A_9H))
      .annule(debut.id(), new Annulation(AUTEUR_LEROY, LE_10_MAI_2026_A_17H, MOTIF_ERREUR_DE_SAISIE));
    assertThat(suivi.activites()).isEmpty();
    SelectionDeSuivis selection = new SelectionDeSuivis(Set.of(EtatDAtelier.EN_COURS), true);
    assertThat(new SuiviDAtelierCriteria(Optional.empty(), selection, LE_10_MAI_2026_A_17H).matches(suivi)).isTrue();
    assertThat(new SuiviDAtelierCriteria(Optional.empty(), Set.of(EtatDAtelier.EN_COURS), LE_10_MAI_2026_A_17H).matches(suivi)).isFalse();
    assertThat(new SuiviDAtelierCriteria(Optional.empty(), selection, LE_10_MAI_2026_A_17H).matches(suiviDAtelierEngage())).isFalse();
    assertThat(
      new SuiviDAtelierCriteria(
        Optional.of(new Periode(LE_11_MAI_2026_A_9H15, LE_11_MAI_2026_A_9H15)),
        selection,
        LE_10_MAI_2026_A_17H
      ).matches(suivi)
    ).isFalse();
  }

  @Test
  void shouldRejectMissingSelection() {
    assertThatThrownBy(() -> new SuiviDAtelierCriteria(Optional.empty(), (SelectionDeSuivis) null, LE_10_MAI_2026_A_17H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("selection");
  }
}

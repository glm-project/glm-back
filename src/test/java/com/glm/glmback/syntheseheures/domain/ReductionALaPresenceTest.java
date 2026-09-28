package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ReductionALaPresenceTest {

  @Test
  void shouldNotBuildWithoutJournees() {
    assertThatThrownBy(() -> new ReductionALaPresence(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journees");
  }

  @Test
  void shouldNotBuildWithNullJournee() {
    List<JourneeDeTravail> journees = Arrays.asList(journeeDuLundiDe8HA17H(), null);

    assertThatThrownBy(() -> new ReductionALaPresence(journees))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("journees");
  }

  /**
   * Un depart referme un travail que l'operateur a oublie d'arreter.
   */
  @Test
  void shouldRefermerAuDepartUnTravailJamaisArrete() {
    ReductionALaPresence reduction = new ReductionALaPresence(List.of(journeeDuLundiDe8HA17H()));

    assertThat(reduction.reduit(travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.empty())))).containsExactly(
      travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H)))
    );
  }

  /**
   * La decision du lot 2 : sans presence, aucun jour ne peut accueillir ce travail sans arbitraire.
   */
  @Test
  void shouldEcarterUnDebutHorsDeTouteJournee() {
    ReductionALaPresence reduction = new ReductionALaPresence(List.of(journeeDuLundiDe8HA17H()));

    assertThat(reduction.reduit(travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_18H, Optional.empty())))).isEmpty();
  }

  /**
   * Le travail est intersecte avec les fenetres de la journee ou il a commence, jamais avec celles du lendemain.
   */
  @Test
  void shouldNeReduireQuALaJourneeOuLeTravailACommence() {
    ReductionALaPresence reduction = new ReductionALaPresence(List.of(journeeDuLundiDe8HA17H(), journeeDuMardiOuverteA8H()));

    assertThat(
      reduction.reduit(travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.of(LE_MARDI_12_MAI_2026_A_9H))))
    ).containsExactly(travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))));
  }

  /**
   * Une journee abandonnee n'a pas de depart : elle contient encore le mardi, mais la venue du mardi, plus recente,
   * l'emporte, comme dans l'atelier.
   */
  @Test
  void shouldRattacherLeTravailALaJourneeLaPlusRecente() {
    JourneeDeTravail abandonnee = journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H));
    ReductionALaPresence reduction = new ReductionALaPresence(List.of(abandonnee, journeeDuMardiOuverteA8H()));

    assertThat(reduction.reduit(travailDuCarter(new Plage(LE_MARDI_12_MAI_2026_A_9H, Optional.empty())))).containsExactly(
      travailDuCarter(new Plage(LE_MARDI_12_MAI_2026_A_9H, Optional.empty()))
    );
  }

  @Test
  void shouldRendrePresumeUnTravailDansUneJourneeAbandonnee() {
    JourneeDeTravail abandonnee = journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H));
    ReductionALaPresence reduction = new ReductionALaPresence(List.of(abandonnee));

    assertThat(reduction.reduit(travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty())))).containsExactly(
      travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true))
    );
  }

  @Test
  void shouldDecouperUnTravailSurLesFenetresDeSaJournee() {
    JourneeDeTravail avecUneCoupure = new JourneeDeTravail(
      List.of(
        arriveeA(LE_LUNDI_11_MAI_2026_A_8H),
        departA(LE_LUNDI_11_MAI_2026_A_12H),
        arriveeA(LE_LUNDI_11_MAI_2026_A_13H),
        departA(LE_LUNDI_11_MAI_2026_A_17H)
      )
    );
    ReductionALaPresence reduction = new ReductionALaPresence(List.of(avecUneCoupure));

    assertThat(reduction.reduit(travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.empty())))).containsExactly(
      travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))),
      travailDuCarter(new Plage(LE_LUNDI_11_MAI_2026_A_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H)))
    );
  }

  private static IntervalleDActivite travailDuCarter(Plage plage) {
    return new IntervalleDActivite(activiteDeTravailDuCarterSurLaDmu50(), plage);
  }
}

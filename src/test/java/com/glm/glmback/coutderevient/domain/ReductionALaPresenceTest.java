package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Le temps brut ramene aux fenetres de presence, sur la regle exacte de l'atelier.
 */
@UnitTest
class ReductionALaPresenceTest {

  private static final Activite FRAISAGE_DE_DUPONT = Activite.builder()
    .operateur(OPERATEUR_ID_DUPONT)
    .poste(Optional.of(POSTE_ID_FRAISEUSE))
    .nature(Optional.of(NATURE_FRAISAGE))
    .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
    .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
    .categorie(CategorieDActivite.TRAVAIL);

  /**
   * La pause de midi scinde le travail en deux, sans qu'elle ait jamais eu besoin d'etre recopiee dans le journal de
   * l'element.
   */
  @Test
  void shouldSplitOnPauseDeMidi() {
    ReductionALaPresence reduction = ReductionALaPresence.de(
      List.of(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())))
    );

    List<IntervalleDActivite> reduits = reduction.reduit(intervalle(LE_11_MAI_A_11H, Optional.of(LE_11_MAI_A_14H)));

    assertThat(reduits)
      .extracting(IntervalleDActivite::plage)
      .containsExactly(new Plage(LE_11_MAI_A_11H, Optional.of(LE_11_MAI_A_12H)), new Plage(LE_11_MAI_A_13H, Optional.of(LE_11_MAI_A_14H)));
  }

  @Test
  void shouldTruncateAtDepart() {
    ReductionALaPresence reduction = ReductionALaPresence.de(
      List.of(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())))
    );

    List<IntervalleDActivite> reduits = reduction.reduit(intervalle(LE_11_MAI_A_14H, Optional.empty()));

    assertThat(reduits).extracting(IntervalleDActivite::plage).containsExactly(new Plage(LE_11_MAI_A_14H, Optional.of(LE_11_MAI_A_17H)));
  }

  /**
   * Un debut qui ne tombe dans aucune journee connue est rendu intact : c'est la presence qui manque, et le domaine
   * ne masque pas l'anomalie derriere un temps ampute.
   */
  @Test
  void shouldKeepIntervalleWithoutAnyJournee() {
    ReductionALaPresence reduction = ReductionALaPresence.de(List.of());

    IntervalleDActivite intervalle = intervalle(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_10H));

    assertThat(reduction.reduit(intervalle)).containsExactly(intervalle);
  }

  @Test
  void shouldKeepIntervalleStartingOutsideAnyJournee() {
    ReductionALaPresence reduction = ReductionALaPresence.de(
      List.of(new PresenceDUnOperateur(OPERATEUR_ID_DUPONT, List.of(journeeDe8HA17HAvecPauseDeMidi())))
    );

    IntervalleDActivite intervalle = intervalle(LE_12_MAI_A_8H, Optional.of(LE_12_MAI_A_8H.plusSeconds(3600)));

    assertThat(reduction.reduit(intervalle)).containsExactly(intervalle);
  }

  /**
   * L'intervalle est borne par la journee ou il a commence : sans cela, un travail jamais arrete courrait jusqu'a la
   * venue suivante.
   */
  @Test
  void shouldBoundToTheJourneeWhereItStarted() {
    ReductionALaPresence reduction = ReductionALaPresence.de(
      List.of(
        new PresenceDUnOperateur(
          OPERATEUR_ID_DUPONT,
          List.of(journeeDe8HA17HAvecPauseDeMidi(), new JourneeDeTravail(List.of(arriveeA(LE_12_MAI_A_8H))))
        )
      )
    );

    List<IntervalleDActivite> reduits = reduction.reduit(intervalle(LE_11_MAI_A_14H, Optional.empty()));

    assertThat(reduits).extracting(IntervalleDActivite::plage).containsExactly(new Plage(LE_11_MAI_A_14H, Optional.of(LE_11_MAI_A_17H)));
  }

  private static IntervalleDActivite intervalle(java.time.Instant debut, Optional<java.time.Instant> fin) {
    return new IntervalleDActivite(FRAISAGE_DE_DUPONT, new Plage(debut, fin));
  }
}

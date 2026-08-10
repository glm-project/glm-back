package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class TempsDAtelierServiceTest {

  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final TempsDAtelierService temps = new TempsDAtelierService(suivis, journees);

  @Test
  void shouldNotReadTempsEffectifDUnSuiviInconnu() {
    SuiviDAtelierId inconnu = SuiviDAtelierId.newId();

    assertThatThrownBy(() -> temps.tempsEffectif(inconnu)).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  /**
   * La pause de midi n'est jamais entree dans le journal de l'element : elle le scinde pourtant en deux, parce que le
   * temps effectif est l'intersection des deux journaux.
   */
  @Test
  void shouldScinderLeTravailAutourDeLaPauseDeMidi() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H))
    );

    assertThat(temps.tempsEffectif(suivi))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  /**
   * L'operateur oublie d'arreter son element et rentre chez lui : son depart referme l'activite, la ou un intervalle
   * brut aurait couru indefiniment.
   */
  @Test
  void shouldRefermerAuDepartUneActiviteJamaisArretee() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H)));

    assertThat(temps.tempsEffectif(suivi))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_13H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
      });
  }

  /**
   * Une seule regularisation de depart corrige tous les elements de la journee, la ou une pause recopiee element par
   * element aurait demande autant de corrections que d'elements.
   */
  @Test
  void shouldRefermerTousLesElementsSurUneSeuleRegularisationDeDepart() {
    JourneeDeTravail ouverte = journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId premier = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    SuiviDAtelierId second = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H)));

    journees.update(ouverte.enregistre(departRegulariseParLeroyA(LE_10_MAI_2026_A_17H)));

    assertThat(temps.tempsEffectif(premier))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
    assertThat(temps.tempsEffectif(second))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldLaisserOuvertUnTravailEnCoursSurUneJourneeEnCours() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(temps.tempsEffectif(suivi)).singleElement().matches(IntervalleDActivite::estOuvert);
  }

  /**
   * Le domaine ne masque pas l'anomalie : sans presence saisie, l'intervalle brut est rendu tel quel, et c'est la
   * presence qui reste a regulariser.
   */
  @Test
  void shouldRendreIntactUnIntervalleSansAucunePresenceConnue() {
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H))
    );

    assertThat(temps.tempsEffectif(suivi))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
      });
  }

  @Test
  void shouldEcarterUnTravailEntierementHorsDesFenetresDePresence() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    assertThat(temps.tempsEffectif(suivi)).isEmpty();
  }

  private SuiviDAtelierId enAtelier(SuiviDAtelier suivi) {
    return suivis.create(suivi).id();
  }
}

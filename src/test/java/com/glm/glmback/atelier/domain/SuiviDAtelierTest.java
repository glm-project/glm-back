package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class SuiviDAtelierTest {

  @ParameterizedTest
  @MethodSource("composantsManquants")
  void shouldNotBuildWithoutMandatoryComposant(Runnable construction, String champ) {
    assertThatThrownBy(construction::run).isExactlyInstanceOf(MissingMandatoryValueException.class).hasMessageContaining(champ);
  }

  @Test
  void shouldEngagerUnElementSansJournalNiCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThat(suivi.element()).isEqualTo(elementEngageOf2026000042());
    assertThat(suivi.engagement()).isEqualTo(engagementParLeroy());
    assertThat(suivi.journal().evenements()).isEmpty();
    assertThat(suivi.cloture()).isEmpty();
    assertThat(suivi.estCloture()).isFalse();
    assertThat(suivi.etat()).isEqualTo(EtatDAtelier.EN_ATTENTE);
  }

  @Test
  void shouldRefuserUnEvenementAnterieurALEngagement() {
    SuiviDAtelier suivi = suiviDAtelierEngage();
    EvenementDAtelier avantLEngagement = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_7H.minusSeconds(1));

    assertThatThrownBy(() -> suivi.enregistre(avantLEngagement))
      .isExactlyInstanceOf(EvenementAvantEngagementException.class)
      .hasMessageContaining("anterieur a l'engagement");
  }

  @Test
  void shouldRefuserUnEvenementPosterieurALaCloture() {
    SuiviDAtelier cloture = suiviDAtelierEngage().cloture(clotureParLeroyA(LE_10_MAI_2026_A_12H));
    EvenementDAtelier apresLaCloture = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);

    assertThatThrownBy(() -> cloture.enregistre(apresLaCloture))
      .isExactlyInstanceOf(SuiviDAtelierClotureException.class)
      .hasMessageContaining("posterieur a la cloture");
  }

  @Test
  void shouldRefermerLIntervallePrecedentSurUneSaisieOubliee() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H));

    SuiviDAtelier regularise = suivi.enregistre(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));

    assertThat(regularise.activites())
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::categorie)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), CategorieDActivite.TRAVAIL),
        tuple(LE_10_MAI_2026_A_12H, Optional.of(LE_10_MAI_2026_A_17H), CategorieDActivite.NON_CONFORMITE)
      );
  }

  /**
   * Deux pieces du meme element sur deux machines : le client le demande explicitement, et chaque poste mene sa propre
   * activite.
   */
  @Test
  void shouldMenerDeuxPostesDeFrontSurLeMemeElement() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H))
      .enregistre(finSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_12H));

    assertThat(suivi.activitesEnCours()).extracting(ActiviteEnCours::poste).containsExactly(Optional.of(POSTE_FRAISEUSE_1));
    assertThat(suivi.activites()).hasSize(2);
  }

  @Test
  void shouldRepasserEnTravailQuandUneNonConformiteEnTropEstAnnulee() {
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)).enregistre(nonConformite);

    SuiviDAtelier corrige = suivi.annule(nonConformite.id(), annulationParLeroy());

    assertThat(corrige.activitesEnCours()).extracting(ActiviteEnCours::categorie).containsExactly(CategorieDActivite.TRAVAIL);
    assertThat(corrige.etat()).isEqualTo(EtatDAtelier.EN_COURS);
  }

  @Test
  void shouldCorrigerUneSaisieFausseEnUnSeulActe() {
    EvenementDAtelier debutFautif = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutFautif).enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));

    SuiviDAtelier corrige = suivi.corrige(
      debutFautif.id(),
      annulationParLeroy(),
      debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_7H30)
    );

    assertThat(corrige.activites())
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_7H30);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_12H);
      });
  }

  @Test
  void shouldRefuserLaMemeCorrectionJoueeEnDeuxTemps() {
    EvenementDAtelier debutFautif = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutFautif).enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));
    Annulation annulation = annulationParLeroy();

    assertThatThrownBy(() -> suivi.annule(debutFautif.id(), annulation)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  @Test
  void shouldFermerLesActivitesOuvertesSurLaCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    SuiviDAtelier cloture = suivi.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));

    assertThat(cloture.etat()).isEqualTo(EtatDAtelier.CLOTURE);
    assertThat(cloture.activitesEnCours()).isEmpty();
    assertThat(cloture.activites())
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldCorrigerUnJournalDejaCloture() {
    EvenementDAtelier debutFautif = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier cloture = suiviDAtelierEngage().enregistre(debutFautif).cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));

    SuiviDAtelier corrige = cloture.corrige(
      debutFautif.id(),
      annulationParLeroy(),
      debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_7H30)
    );

    assertThat(corrige.activites())
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_7H30));
  }

  @Test
  void shouldRouvrirUnSuiviEnAnnulantSaCloture() {
    SuiviDAtelier cloture = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));

    SuiviDAtelier rouvert = cloture.annuleLaCloture();

    assertThat(rouvert.estCloture()).isFalse();
    assertThat(rouvert.etat()).isEqualTo(EtatDAtelier.EN_COURS);
  }

  @Test
  void shouldBeEnAttenteQuandTousLesEvenementsSontAnnules() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    SuiviDAtelier corrige = suiviDAtelierEngage().enregistre(debut).annule(debut.id(), annulationParLeroy());

    assertThat(corrige.etat()).isEqualTo(EtatDAtelier.EN_ATTENTE);
  }

  @Test
  void shouldBeInterrompuQuandToutesLesActivitesSontCloses() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));

    assertThat(suivi.etat()).isEqualTo(EtatDAtelier.INTERROMPU);
  }

  private static Stream<Arguments> composantsManquants() {
    SuiviDAtelierId id = SuiviDAtelierId.newId();

    return Stream.of(
      construction(() -> suivi(null, elementEngageOf2026000042(), engagementParLeroy(), JournalDAtelier.vide(), Optional.empty()), "id"),
      construction(() -> suivi(id, null, engagementParLeroy(), JournalDAtelier.vide(), Optional.empty()), "element"),
      construction(() -> suivi(id, elementEngageOf2026000042(), null, JournalDAtelier.vide(), Optional.empty()), "engagement"),
      construction(() -> suivi(id, elementEngageOf2026000042(), engagementParLeroy(), null, Optional.empty()), "journal"),
      construction(() -> suivi(id, elementEngageOf2026000042(), engagementParLeroy(), JournalDAtelier.vide(), null), "cloture")
    );
  }

  private static Arguments construction(Runnable construction, String champ) {
    return Arguments.of(construction, champ);
  }

  private static void suivi(
    SuiviDAtelierId id,
    ElementEngage element,
    Engagement engagement,
    JournalDAtelier journal,
    Optional<Cloture> cloture
  ) {
    new SuiviDAtelier(id, element, engagement, journal, cloture);
  }
}

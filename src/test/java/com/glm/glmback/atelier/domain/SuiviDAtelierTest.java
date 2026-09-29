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
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H));

    SuiviDAtelier regularise = suivi.enregistre(passageEnNonConformiteDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(regularise.activites())
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::categorie)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), CategorieDActivite.TRAVAIL),
        tuple(LE_10_MAI_2026_A_12H, Optional.of(LE_10_MAI_2026_A_17H), CategorieDActivite.NON_CONFORMITE),
        tuple(LE_10_MAI_2026_A_17H, Optional.empty(), CategorieDActivite.TRAVAIL)
      );
  }

  /**
   * Une transition termine l'activite qu'elle vise et en ouvre une distincte, de l'autre categorie : c'est elle qui
   * est desormais en cours.
   */
  @Test
  void shouldOuvrirUneActiviteDistincteParUneTransition() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);

    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite);

    assertThat(suivi.activites())
      .extracting(IntervalleDActivite::evenement, IntervalleDActivite::fin)
      .containsExactly(tuple(travail.id(), Optional.of(LE_10_MAI_2026_A_12H)), tuple(nonConformite.id(), Optional.empty()));
    assertThat(suivi.activitesEnCours())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.categorie()).isEqualTo(CategorieDActivite.NON_CONFORMITE);
        assertThat(activite.depuis()).isEqualTo(LE_10_MAI_2026_A_12H);
      });
  }

  /**
   * Une fin ne termine que l'activite qu'elle vise : celle qu'une relance a remplacee ne lui laisse rien a terminer,
   * et la relance reste en cours.
   */
  @Test
  void shouldNeTerminerQueLActiviteViseeParUneFin() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier relance = suiviDAtelierEngage().enregistre(premiere).enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H));
    EvenementDAtelier finDeLaPremiere = finDe(premiere).a(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> relance.enregistre(finDeLaPremiere)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
    assertThat(relance.activitesEnCours()).extracting(ActiviteEnCours::depuis).containsExactly(LE_10_MAI_2026_A_9H);
  }

  @Test
  void shouldRefuserUnGesteQuiViseUneActiviteAbsenteDuSuivi() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    EvenementDAtelier finDAilleurs = finDe(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)).a(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> suivi.exigeLActiviteViseePar(finDAilleurs)).isExactlyInstanceOf(ActiviteViseeIntrouvableException.class);
  }

  /**
   * Deux pieces du meme element sur deux machines : le client le demande explicitement, et chaque poste mene sa propre
   * activite.
   */
  @Test
  void shouldMenerDeuxPostesDeFrontSurLeMemeElement() {
    EvenementDAtelier debutSurFraiseuse2 = debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .enregistre(debutSurFraiseuse2)
      .enregistre(finDe(debutSurFraiseuse2).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.activitesEnCours()).extracting(ActiviteEnCours::poste).containsExactly(Optional.of(POSTE_ID_FRAISEUSE_1));
    assertThat(suivi.activites()).hasSize(2);
  }

  @Test
  void shouldRepasserEnTravailQuandUneNonConformiteEnTropEstAnnulee() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(debut).a(LE_10_MAI_2026_A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(nonConformite);

    SuiviDAtelier corrige = suivi.annule(nonConformite.id(), annulationParLeroy());

    assertThat(corrige.activitesEnCours()).extracting(ActiviteEnCours::categorie).containsExactly(CategorieDActivite.TRAVAIL);
    assertThat(corrige.etat()).isEqualTo(EtatDAtelier.EN_COURS);
  }

  @Test
  void shouldCorrigerUneSaisieFausseEnUnSeulActe() {
    EvenementDAtelier debutFautif = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutFautif).enregistre(finDe(debutFautif).a(LE_10_MAI_2026_A_12H));

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
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutFautif).enregistre(finDe(debutFautif).a(LE_10_MAI_2026_A_12H));
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
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.etat()).isEqualTo(EtatDAtelier.INTERROMPU);
  }

  @Test
  void shouldGarderUneSeuleActiviteEnCoursApresUneRelance() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H));

    assertThat(suivi.etat()).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(suivi.activitesEnCours())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.activite()).isEqualTo(cleDeFraiseuse1DeDupont());
        assertThat(activite.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
        assertThat(activite.depuis()).isEqualTo(LE_10_MAI_2026_A_13H);
      });
  }

  @Test
  void shouldEtreInterrompuQuandUneActiviteRelanceeEstArretee() {
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .enregistre(relance)
      .enregistre(finDe(relance).a(LE_10_MAI_2026_A_17H));

    assertThat(suivi.etat()).isEqualTo(EtatDAtelier.INTERROMPU);
    assertThat(suivi.activitesEnCours()).isEmpty();
  }

  @Test
  void shouldRefuserUneRelanceApresLaCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .cloture(clotureParLeroyA(LE_10_MAI_2026_A_12H));
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);

    assertThatThrownBy(() -> suivi.enregistre(relance)).isExactlyInstanceOf(SuiviDAtelierClotureException.class);
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

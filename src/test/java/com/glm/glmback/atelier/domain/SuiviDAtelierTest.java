package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class SuiviDAtelierTest {

  private static final Instant A_20H59 = Instant.parse("2026-05-10T20:59:00Z");
  private static final Instant A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant LE_11_MAI_2026_A_1H = Instant.parse("2026-05-11T01:00:00Z");

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
    assertThat(suivi.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.EN_ATTENTE);
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

  /**
   * Une fin ferme l'activite en cours et l'ouverture de la meme heure en ouvre une distincte, de l'autre categorie :
   * c'est elle qui est desormais en cours.
   */
  @Test
  void shouldOuvrirUneActiviteDistincteApresUneFinDeLaMemeHeure() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);

    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .enregistre(nonConformite);

    assertThat(suivi.activites())
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(travail, Optional.of(LE_10_MAI_2026_A_12H)), tuple(nonConformite, Optional.empty()));
    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_17H))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.categorie()).isEqualTo(CategorieDActivite.NON_CONFORMITE);
        assertThat(activite.depuis()).isEqualTo(LE_10_MAI_2026_A_12H);
      });
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

    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_17H))
      .extracting(ActiviteEnCours::poste)
      .containsExactly(Optional.of(POSTE_ID_FRAISEUSE_1));
    assertThat(suivi.activites()).hasSize(2);
  }

  @Test
  void shouldFermerLesActivitesOuvertesSurLaCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    SuiviDAtelier cloture = suivi.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));

    assertThat(cloture.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.CLOTURE);
    assertThat(cloture.activitesEnCours(LE_10_MAI_2026_A_17H)).isEmpty();
    assertThat(cloture.activites())
      .singleElement()
      .satisfies(activite -> assertThat(activite.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldRouvrirUnSuiviEnAnnulantSaCloture() {
    SuiviDAtelier cloture = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));

    SuiviDAtelier rouvert = cloture.annuleLaCloture();

    assertThat(rouvert.estCloture()).isFalse();
    assertThat(rouvert.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.EN_COURS);
  }

  @Test
  void shouldBeInterrompuQuandToutesLesActivitesSontCloses() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.INTERROMPU);
  }

  /**
   * Activite a 08:00, lecture a 20:59 : l'element est en cours, et l'activite parmi les activites en cours.
   */
  @Test
  void shouldEtreEnCoursJusquALEcheanceDeSonActivite() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    assertThat(suivi.etat(A_20H59)).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(suivi.activitesEnCours(A_20H59)).extracting(ActiviteEnCours::depuis).containsExactly(LE_10_MAI_2026_A_8H);
    assertThat(intervalles(suivi, A_20H59)).singleElement().matches(IntervalleDActivite::estOuvert);
  }

  /**
   * Activite a 08:00, aucune fin a 21:00 : l'activite est terminee automatiquement a 21:00, avec une anomalie, et sort
   * des activites en cours. Lue le lendemain, elle garde la meme borne.
   */
  @Test
  void shouldTerminerAutomatiquementALEcheanceUneActiviteQueRienNATerminee() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    for (Instant lecture : List.of(A_21H, LE_11_MAI_2026_A_9H15)) {
      assertThat(suivi.etat(lecture)).describedAs("lecture %s", lecture).isEqualTo(EtatDAtelier.INTERROMPU);
      assertThat(suivi.activitesEnCours(lecture)).describedAs("lecture %s", lecture).isEmpty();
      assertThat(intervalles(suivi, lecture))
        .describedAs("lecture %s", lecture)
        .singleElement()
        .satisfies(intervalle -> {
          assertThat(intervalle.fin()).contains(A_21H);
          assertThat(intervalle.finAutomatique()).isTrue();
        });
    }
  }

  /**
   * Travail 08 h puis NC 12 h : les 4 h de travail sont terminees a 12 h, sans anomalie ; la non conformite est en
   * cours, jusqu'a sa propre echeance, a 01 h le lendemain.
   */
  @Test
  void shouldRelancerLEcheanceAChaqueOuverture() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .enregistre(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));

    assertThat(intervalles(suivi, A_21H))
      .extracting(IntervalleDActivite::categorie, IntervalleDActivite::fin, IntervalleDActivite::finAutomatique)
      .containsExactly(
        tuple(CategorieDActivite.TRAVAIL, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(CategorieDActivite.NON_CONFORMITE, Optional.empty(), false)
      );
    assertThat(intervalles(suivi, LE_11_MAI_2026_A_1H).getLast().fin()).contains(LE_11_MAI_2026_A_1H);
  }

  /**
   * Fin puis NC de 12 h recues le lendemain : rejouees a leur heure metier, elles terminent le travail a 12 h et en
   * retirent l'anomalie ; la non conformite, lue le lendemain, porte la sienne, a 01 h.
   */
  @Test
  void shouldRejouerASonHeureMetierUneFinEtUneOuvertureRecuesLeLendemain() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    SuiviDAtelier rejoue = suivi
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .enregistre(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));

    assertThat(intervalles(rejoue, LE_11_MAI_2026_A_9H15))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::finAutomatique)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(LE_10_MAI_2026_A_12H, Optional.of(LE_11_MAI_2026_A_1H), true)
      );
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
    new SuiviDAtelier(id, element, engagement, journal, cloture, new RevisionDuSuivi(0));
  }

  private static List<IntervalleDActivite> intervalles(SuiviDAtelier suivi, Instant evaluation) {
    return suivi
      .activites()
      .stream()
      .map(activite -> activite.a(evaluation))
      .toList();
  }
}

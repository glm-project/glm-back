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
  private static final Instant A_22H = Instant.parse("2026-05-10T22:00:00Z");
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

  @Test
  void shouldRefermerLIntervallePrecedentSurUneSaisieOubliee() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H));

    SuiviDAtelier regularise = suivi.enregistre(passageEnNonConformiteDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(regularise.activites())
      .extracting(Activite::debut, Activite::fin, Activite::categorie)
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
   * Une fin ne termine que l'activite qu'elle vise : celle qu'une relance a remplacee ne lui laisse rien a terminer.
   * Conservee, elle laisse la premiere activite et sa relance a resoudre : plus rien n'est en cours, et le suivi, juge
   * sur ses seules activites interpretables, est interrompu.
   */
  @Test
  void shouldNeTerminerQueLActiviteViseeParUneFin() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier seconde = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    SuiviDAtelier relance = suiviDAtelierEngage().enregistre(premiere).enregistre(seconde);
    EvenementDAtelier finDeLaPremiere = finDe(premiere).a(LE_10_MAI_2026_A_12H);

    SuiviDAtelier conserve = relance.enregistre(finDeLaPremiere);

    assertThat(conserve.activitesEnCours(LE_10_MAI_2026_A_17H)).isEmpty();
    assertThat(conserve.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.INTERROMPU);
    assertThat(conserve.conflits())
      .singleElement()
      .satisfies(conflit -> {
        assertThat(conflit.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
        assertThat(conflit.poste()).contains(POSTE_ID_FRAISEUSE_1);
        assertThat(conflit.activites()).containsExactly(premiere.activite().orElseThrow(), seconde.activite().orElseThrow());
        assertThat(conflit.pointages()).containsExactly(premiere.id(), seconde.id(), finDeLaPremiere.id());
      });
  }

  /**
   * L'etat se juge sur les seules activites interpretables : l'ouverture pointee apres une sequence en conflit est en
   * cours, et le suivi avec elle.
   */
  @Test
  void shouldEtreEnCoursParUneActiviteInterpretableMalgreUneSequenceEnConflit() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nouvelle = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(premiere)
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H))
      .enregistre(finDe(premiere).a(LE_10_MAI_2026_A_12H))
      .enregistre(nouvelle);

    assertThat(suivi.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_17H))
      .extracting(ActiviteEnCours::ouverture)
      .containsExactly(nouvelle.activite().orElseThrow());
    assertThat(suivi.conflits()).hasSize(1);
  }

  /**
   * Une activite a resoudre n'a ni fin ni fin automatique, meme lue apres son echeance : la regle des 13 h ne tranche
   * pas un conflit.
   */
  @Test
  void shouldLireSansFinNiFinAutomatiqueUneActiviteAResoudre() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(premiere)
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H))
      .enregistre(finDe(premiere).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.intervalles(LE_11_MAI_2026_A_9H15))
      .hasSize(2)
      .allSatisfy(intervalle -> {
        assertThat(intervalle.aResoudre()).isTrue();
        assertThat(intervalle.fin()).isEmpty();
        assertThat(intervalle.finAutomatique()).isFalse();
      });
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

    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_17H))
      .extracting(ActiviteEnCours::poste)
      .containsExactly(Optional.of(POSTE_ID_FRAISEUSE_1));
    assertThat(suivi.activites()).hasSize(2);
  }

  @Test
  void shouldRepasserEnTravailQuandUneNonConformiteEnTropEstAnnulee() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(debut).a(LE_10_MAI_2026_A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(nonConformite);

    SuiviDAtelier corrige = suivi.annule(nonConformite.id(), annulationParLeroy());

    assertThat(corrige.activitesEnCours(LE_10_MAI_2026_A_17H))
      .extracting(ActiviteEnCours::categorie)
      .containsExactly(CategorieDActivite.TRAVAIL);
    assertThat(corrige.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.EN_COURS);
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
      .satisfies(activite -> {
        assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_7H30);
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_12H);
      });
  }

  @Test
  void shouldRefuserDeDeplacerUnOuvrantEncoreViseParUneFinSurSonPoste() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_12H));
    EvenementDAtelier remplacementSurFraiseuse2 = debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> suivi.corrige(debut.id(), annulationParLeroy(), remplacementSurFraiseuse2))
      .isExactlyInstanceOf(ActiviteViseeIncoherenteException.class)
      .hasMessageContaining(debut.activite().orElseThrow().uuid().toString());
  }

  @Test
  void shouldPouvoirDeplacerUnOuvrantApresAnnulationDeLaFinQuiLeVisait() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(fin).annule(fin.id(), annulationParLeroy());

    SuiviDAtelier corrige = suivi.corrige(debut.id(), annulationParLeroy(), debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_8H));

    assertThat(corrige.conflits()).isEmpty();
    assertThat(corrige.activites())
      .singleElement()
      .satisfies(activite -> assertThat(activite.cle().poste()).contains(POSTE_ID_FRAISEUSE_2));
  }

  /**
   * La meme correction jouee en deux temps commence par un etat intermediaire en conflit, admis : annuler le debut
   * laisse sa fin sans activite a terminer.
   */
  @Test
  void shouldAdmettreEnConflitLAnnulationDUnDebutQueViseUneFin() {
    EvenementDAtelier debutFautif = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutFautif).enregistre(finDe(debutFautif).a(LE_10_MAI_2026_A_12H));

    SuiviDAtelier annule = suivi.annule(debutFautif.id(), annulationParLeroy());

    assertThat(annule.conflits())
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.activites()).isEmpty());
    assertThat(annule.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.INTERROMPU);
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
      .satisfies(activite -> assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_7H30));
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
  void shouldBeEnAttenteQuandTousLesEvenementsSontAnnules() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    SuiviDAtelier corrige = suiviDAtelierEngage().enregistre(debut).annule(debut.id(), annulationParLeroy());

    assertThat(corrige.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.EN_ATTENTE);
  }

  @Test
  void shouldBeInterrompuQuandToutesLesActivitesSontCloses() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.INTERROMPU);
  }

  @Test
  void shouldGarderUneSeuleActiviteEnCoursApresUneRelance() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H));

    assertThat(suivi.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_17H))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
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

    assertThat(suivi.etat(LE_10_MAI_2026_A_17H)).isEqualTo(EtatDAtelier.INTERROMPU);
    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_17H)).isEmpty();
  }

  @Test
  void shouldRefuserUneRelanceApresLaCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
      .cloture(clotureParLeroyA(LE_10_MAI_2026_A_12H));
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);

    assertThatThrownBy(() -> suivi.enregistre(relance)).isExactlyInstanceOf(SuiviDAtelierClotureException.class);
  }

  /**
   * Activite a 08:00, lecture a 20:59 : l'element est en cours, et l'activite parmi les activites en cours.
   */
  @Test
  void shouldEtreEnCoursJusquALEcheanceDeSonActivite() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    assertThat(suivi.etat(A_20H59)).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(suivi.activitesEnCours(A_20H59)).extracting(ActiviteEnCours::depuis).containsExactly(LE_10_MAI_2026_A_8H);
    assertThat(suivi.intervalles(A_20H59)).singleElement().matches(IntervalleDActivite::estOuvert);
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
      assertThat(suivi.intervalles(lecture))
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
  void shouldRelancerLEcheanceAChaqueTransition() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.intervalles(A_21H))
      .extracting(IntervalleDActivite::categorie, IntervalleDActivite::fin, IntervalleDActivite::finAutomatique)
      .containsExactly(
        tuple(CategorieDActivite.TRAVAIL, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(CategorieDActivite.NON_CONFORMITE, Optional.empty(), false)
      );
    assertThat(suivi.intervalles(LE_11_MAI_2026_A_1H).getLast().fin()).contains(LE_11_MAI_2026_A_1H);
  }

  /**
   * Transition NC 12 h recue le lendemain : rejouee a son heure metier, elle termine le travail a 12 h et en retire
   * l'anomalie ; la non conformite, lue le lendemain, porte la sienne, a 01 h.
   */
  @Test
  void shouldRejouerASonHeureMetierUneTransitionRecueLeLendemain() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    SuiviDAtelier rejoue = suivi.enregistre(passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThat(rejoue.intervalles(LE_11_MAI_2026_A_9H15))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::finAutomatique)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(LE_10_MAI_2026_A_12H, Optional.of(LE_11_MAI_2026_A_1H), true)
      );
  }

  /**
   * Debut corrige de 08 h a 12 h, lecture a 22 h : l'echeance passe de 21 h a 01 h ; l'activite redevient en cours et
   * perd son anomalie.
   */
  @Test
  void shouldRedevenirEnCoursQuandLaCorrectionDuDebutRepousseLEcheance() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut);
    assertThat(suivi.etat(A_22H)).isEqualTo(EtatDAtelier.INTERROMPU);

    SuiviDAtelier corrige = suivi.corrige(debut.id(), annulationParLeroy(), debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_12H));

    assertThat(corrige.etat(A_22H)).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(corrige.activitesEnCours(A_22H)).extracting(ActiviteEnCours::depuis).containsExactly(LE_10_MAI_2026_A_12H);
    assertThat(corrige.intervalles(A_22H))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.estOuvert()).isTrue();
        assertThat(intervalle.finAutomatique()).isFalse();
      });
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

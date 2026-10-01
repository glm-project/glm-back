package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class FeuillesDeTempsServiceTest {

  private static final OperateursConnus REFERENTIEL = id ->
    Optional.of(OPERATEUR_CONNU_DUPONT).filter(operateur -> operateur.id().equals(id));
  private static final FuseauHoraireDeLEntreprise A_PARIS = () -> ZONE_PARIS;

  @Test
  void shouldNotLireLHistoriqueDUnOperateurInconnu() {
    FeuillesDeTempsService service = service(ActivitesEnMemoire.sansActivite(), LE_MARDI_12_MAI_2026_A_10H);

    assertThatThrownBy(() -> service.historique(OPERATEUR_ID_MARTIN, SEMAINE_20_DE_2026))
      .isExactlyInstanceOf(OperateurInconnuException.class)
      .hasMessageContaining(OPERATEUR_ID_MARTIN.uuid().toString());
  }

  @Test
  void shouldPorterLIdentiteRelueEtLaSemaineDemandee() {
    FeuilleDeTemps feuille = historiqueDeDupont(ActivitesEnMemoire.sansActivite());

    assertThat(feuille.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(feuille.semaine()).isEqualTo(SEMAINE_20_DE_2026);
  }

  /**
   * Sept jours toujours, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il manque une journee
   * ou si l'operateur n'etait pas la.
   */
  @Test
  void shouldRendreLesSeptJoursDeLaSemaineSansActivite() {
    FeuilleDeTemps feuille = historiqueDeDupont(ActivitesEnMemoire.sansActivite());

    assertThat(feuille.jours())
      .extracting(JourDeLaSemaine::jour)
      .containsExactly(
        LocalDate.of(2026, 5, 11),
        LocalDate.of(2026, 5, 12),
        LocalDate.of(2026, 5, 13),
        LocalDate.of(2026, 5, 14),
        LocalDate.of(2026, 5, 15),
        LocalDate.of(2026, 5, 16),
        LocalDate.of(2026, 5, 17)
      );
    assertThat(feuille.jours()).allSatisfy(jour -> assertThat(jour.activites()).isEmpty());
  }

  @Test
  void shouldDemanderLesActivitesRecouvrantLesBornesCalendairesDeLaSemaine() {
    ActivitesEnMemoire activites = ActivitesEnMemoire.sansActivite();

    historiqueDeDupont(activites);

    assertThat(activites.debutDemande()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(activites.finExclusiveDemandee()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  @Test
  void shouldGarderLesBornesDuTravailTermineSansArrivee() {
    FeuilleDeTemps feuille = historiqueDeDupont(ActivitesEnMemoire.avec(List.of(travailDuCarterDe8HA10H())));

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.activite()).isEqualTo(activiteDeTravailDuCarterSurLaDmu50());
        assertThat(intervalle.plage()).isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_10H)));
        assertThat(intervalle.lecture().id()).isEqualTo(ACTIVITE_ID_DU_CARTER);
        assertThat(intervalle.lecture().etat()).isEqualTo(EtatDActivite.TERMINEE);
      });
  }

  @Test
  void shouldGarderEnCoursLeTravailAvantSonEcheanceSansFinFabriquee() {
    FeuilleDeTemps feuille = avecTravailLuA(Instant.parse("2026-05-11T18:59:00Z"));

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.plage().fin()).isEmpty();
        assertThat(intervalle.lecture().etat()).isEqualTo(EtatDActivite.EN_COURS);
        assertThat(intervalle.lecture().plage().fin()).isEmpty();
      });
  }

  @Test
  void shouldTerminerAutomatiquementLeTravailAEcheancePile() {
    FeuilleDeTemps feuille = avecTravailLuA(Instant.parse("2026-05-11T19:00:00Z"));

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.plage().fin()).contains(Instant.parse("2026-05-11T19:00:00Z"));
        assertThat(intervalle.lecture().etat()).isEqualTo(EtatDActivite.TERMINEE_AUTOMATIQUEMENT);
        assertThat(intervalle.lecture().plage().fin()).contains(Instant.parse("2026-05-11T19:00:00Z"));
      });
  }

  @Test
  void shouldGarderLaBorneAutomatiqueALaLectureSuivante() {
    FeuilleDeTemps feuille = avecTravailLuA(LE_MARDI_12_MAI_2026_A_10H);

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.plage().fin()).contains(Instant.parse("2026-05-11T19:00:00Z"));
        assertThat(intervalle.lecture().etat()).isEqualTo(EtatDActivite.TERMINEE_AUTOMATIQUEMENT);
      });
  }

  @Test
  void shouldGarderUneFinReelleRegulariseeAuDelaDeLEcheance() {
    ActiviteInterpretee travail = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_23H)))
      .echeance(Instant.parse("2026-05-11T19:00:00Z"))
      .finAuPlusTard(Optional.empty());

    FeuilleDeTemps feuille = historiqueDeDupont(ActivitesEnMemoire.avec(List.of(travail)));

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.plage().fin()).contains(LE_LUNDI_11_MAI_2026_A_23H);
        assertThat(intervalle.lecture().etat()).isEqualTo(EtatDActivite.TERMINEE);
      });
  }

  @Test
  void shouldLaisserAResoudreUneActiviteQueLEcheanceNeTerminePas() {
    ActiviteInterpretee conflit = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()))
      .echeance(Instant.parse("2026-05-11T19:00:00Z"))
      .finAuPlusTard(Optional.of(Instant.parse("2026-05-11T19:00:00Z")));

    FeuilleDeTemps feuille = historiqueDeDupont(ActivitesEnMemoire.avec(List.of(conflit)));

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.plage().fin()).isEmpty();
        assertThat(intervalle.lecture().etat()).isEqualTo(EtatDActivite.A_RESOUDRE);
      });
  }

  @Test
  void shouldLimiterLesJoursPossiblesALInstantSansPlageInversee() {
    ActiviteInterpretee conflit = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()))
      .echeance(Instant.parse("2026-05-11T19:00:00Z"))
      .finAuPlusTard(Optional.of(Instant.parse("2026-05-13T08:00:00Z")));
    FeuillesDeTempsService service = service(ActivitesEnMemoire.avec(List.of(conflit)), LE_MARDI_12_MAI_2026_A_10H);

    FeuilleDeTemps avant = service.historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026, Optional.of(Instant.parse("2026-05-11T05:00:00Z")));
    assertThat(avant.jours()).allSatisfy(jour -> assertThat(jour.activites()).isEmpty());
    FeuilleDeTemps pendant = service.historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
    assertThat(activitesDu(pendant, LUNDI_11_MAI_2026)).hasSize(1);
    assertThat(activitesDu(pendant, MARDI_12_MAI_2026)).hasSize(1);
    assertThat(activitesDu(pendant, LocalDate.parse("2026-05-13"))).isEmpty();
  }

  @Test
  void shouldReleverUneSeuleFoisLInstantDeLecture() {
    java.util.concurrent.atomic.AtomicInteger lectures = new java.util.concurrent.atomic.AtomicInteger();
    FeuillesDeTempsService service = FeuillesDeTempsService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(A_PARIS)
      .activites(ActivitesEnMemoire.avec(List.of(travailDuCarterOuvertA8H())))
      .clock(() -> lectures.getAndIncrement() == 0 ? Instant.parse("2026-05-11T18:59:00Z") : LE_MARDI_12_MAI_2026_A_10H);

    FeuilleDeTemps feuille = service.historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(lectures.get()).isEqualTo(1);
    assertThat(feuille.evaluation()).isEqualTo(Instant.parse("2026-05-11T18:59:00Z"));
    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026).getFirst().lecture().etat()).isEqualTo(EtatDActivite.EN_COURS);
  }

  @Test
  void shouldAppliquerLInstantChoisiAuDecoupageEnRelevantLeServeurUneSeuleFois() {
    java.util.concurrent.atomic.AtomicInteger lectures = new java.util.concurrent.atomic.AtomicInteger();
    ActiviteInterpretee nuit = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(Instant.parse("2026-05-10T20:00:00Z"), Optional.empty()))
      .echeance(Instant.parse("2026-05-11T09:00:00Z"))
      .finAuPlusTard(Optional.empty());
    FeuillesDeTempsService service = FeuillesDeTempsService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(A_PARIS)
      .activites(ActivitesEnMemoire.avec(List.of(nuit)))
      .clock(() -> lectures.getAndIncrement() == 0 ? Instant.parse("2026-05-11T09:00:05Z") : LE_MARDI_12_MAI_2026_A_10H);

    FeuilleDeTemps feuille = service.historique(
      OPERATEUR_ID_DUPONT,
      SEMAINE_20_DE_2026,
      Optional.of(Instant.parse("2026-05-10T23:00:00Z"))
    );

    assertThat(lectures.get()).isEqualTo(1);
    assertThat(feuille.evaluation()).isEqualTo(Instant.parse("2026-05-10T23:00:00Z"));
    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.lecture().etat()).isEqualTo(EtatDActivite.EN_COURS);
        assertThat(intervalle.plage().debut()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
        assertThat(intervalle.plage().fin()).isEmpty();
      });
    assertThat(activitesDu(feuille, MARDI_12_MAI_2026)).isEmpty();
  }

  @Test
  void shouldDepartagerDeuxActivitesSimultaneesParLElementPuisParLIdentite() {
    ActiviteInterpretee bride = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DE_LA_BRIDE)
      .activite(activiteDeTravailDeLaBrideSurLaDmu50())
      .plage(travailDuCarterDe8HA10H().plage())
      .echeance(travailDuCarterDe8HA10H().echeance())
      .finAuPlusTard(Optional.empty());
    ActiviteInterpretee autreTravailDuCarter = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DE_LA_BRIDE)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(travailDuCarterDe8HA10H().plage())
      .echeance(travailDuCarterDe8HA10H().echeance())
      .finAuPlusTard(Optional.empty());
    FeuilleDeTemps feuille = historiqueDeDupont(ActivitesEnMemoire.avec(List.of(bride, autreTravailDuCarter, travailDuCarterDe8HA10H())));

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .extracting(intervalle -> intervalle.activite().element())
      .containsExactly(ELEMENT_ID_CARTER, ELEMENT_ID_CARTER, ELEMENT_ID_BRIDE);
    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .extracting(intervalle -> intervalle.lecture().id())
      .containsExactly(ACTIVITE_ID_DU_CARTER, ACTIVITE_ID_DE_LA_BRIDE, ACTIVITE_ID_DE_LA_BRIDE);
  }

  private static FeuilleDeTemps avecTravailLuA(Instant evaluation) {
    return service(ActivitesEnMemoire.avec(List.of(travailDuCarterOuvertA8H())), evaluation).historique(
      OPERATEUR_ID_DUPONT,
      SEMAINE_20_DE_2026
    );
  }

  private static FeuilleDeTemps historiqueDeDupont(ActivitesEnMemoire activites) {
    return service(activites, LE_MARDI_12_MAI_2026_A_10H).historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
  }

  private static FeuillesDeTempsService service(ActivitesEnMemoire activites, Instant maintenant) {
    return FeuillesDeTempsService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(A_PARIS)
      .activites(activites)
      .clock(() -> maintenant);
  }

  private static List<IntervalleDActivite> activitesDu(FeuilleDeTemps feuille, LocalDate jour) {
    return feuille
      .jours()
      .stream()
      .filter(jourDeLaSemaine -> jourDeLaSemaine.jour().equals(jour))
      .findFirst()
      .orElseThrow()
      .activites();
  }
}

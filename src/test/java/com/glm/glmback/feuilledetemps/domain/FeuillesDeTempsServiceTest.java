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
  private static final PointagesDAtelier AUCUN_POINTAGE = (operateur, periode) -> Optional.empty();

  @Test
  void shouldNotLireLHistoriqueDUnOperateurInconnu() {
    FeuillesDeTempsService service = service(PresencesEnMemoire.sansJournee(), AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H);

    assertThatThrownBy(() -> service.historique(OPERATEUR_ID_MARTIN, SEMAINE_20_DE_2026))
      .isExactlyInstanceOf(OperateurInconnuException.class)
      .hasMessageContaining(OPERATEUR_ID_MARTIN.uuid().toString());
  }

  @Test
  void shouldPorterLIdentiteRelueEtLaSemaineDemandee() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.sansJournee());

    assertThat(feuille.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(feuille.semaine()).isEqualTo(SEMAINE_20_DE_2026);
  }

  /**
   * Sept jours toujours, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il manque une journee
   * ou si l'operateur n'etait pas la.
   */
  @Test
  void shouldRendreLesSeptJoursDeLaSemaineSansPresence() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.sansJournee());

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
    assertThat(feuille.jours()).allSatisfy(jour -> assertThat(jour.presence()).isEmpty());
  }

  @Test
  void shouldDemanderLesJourneesRecouvrantLaSemaineDansLaZoneDeLEntreprise() {
    PresencesEnMemoire presences = PresencesEnMemoire.sansJournee();

    historiqueDeDupont(presences);

    assertThat(presences.debutDemande()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(presences.finExclusiveDemandee()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  @Test
  void shouldRattacherLesFenetresAuJourQuiLesPorte() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())));

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))
    );
    assertThat(presenceDu(feuille, MARDI_12_MAI_2026)).isEmpty();
  }

  /**
   * Une equipe de nuit compte sur deux jours : c'est la raison d'etre de ce contexte, l'atelier ne connaissant que
   * l'arrivee et le depart.
   */
  @Test
  void shouldScinderUneJourneeAChevalSurMinuit() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundi22HAuMardi2H())));

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_22H, Optional.of(LE_MARDI_12_MAI_2026_A_MINUIT))
    );
    assertThat(presenceDu(feuille, MARDI_12_MAI_2026)).containsExactly(
      new Plage(LE_MARDI_12_MAI_2026_A_MINUIT, Optional.of(LE_MARDI_12_MAI_2026_A_2H))
    );
  }

  @Test
  void shouldLaisserOuverteLaFenetreDUneJourneeSansDepart() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuMardiOuverteA8H())));

    assertThat(presenceDu(feuille, MARDI_12_MAI_2026)).containsExactly(new Plage(LE_MARDI_12_MAI_2026_A_8H, Optional.empty()));
    assertThat(presenceDu(feuille, MERCREDI_13_MAI_2026)).isEmpty();
  }

  @Test
  void shouldIgnorerUneJourneeHorsDeLaSemaine() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuDimanchePrecedentDe8HA17H())));

    assertThat(feuille.jours()).allSatisfy(jour -> assertThat(jour.presence()).isEmpty());
  }

  /**
   * L'ordre des journees rendues par le port ne doit rien decider : deux venues le meme jour se lisent dans l'ordre
   * des heures.
   */
  @Test
  void shouldTrierLesFenetresDUnJourParHeure() {
    PresencesEnMemoire presences = PresencesEnMemoire.avec(
      List.of(journeeDuMardiOuverteA8H(), journeeDuLundi22HAuMardi2H(), journeeDuLundiDe8HA17H())
    );

    FeuilleDeTemps feuille = historiqueDeDupont(presences);

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026))
      .extracting(Plage::debut)
      .containsExactly(LE_LUNDI_11_MAI_2026_A_8H, LE_LUNDI_11_MAI_2026_A_22H);
    assertThat(presenceDu(feuille, MARDI_12_MAI_2026))
      .extracting(Plage::debut)
      .containsExactly(LE_MARDI_12_MAI_2026_A_MINUIT, LE_MARDI_12_MAI_2026_A_8H);
  }

  /**
   * E2 : lundi sans depart, lu mardi. La journee s'arrete a la fin de l'OF 43 a 16:00, et la derniere plage le dit.
   */
  @Test
  void shouldSignalerLaPlagePresumeeDUneJourneeAbandonnee() {
    FeuilleDeTemps feuille = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe7HSansDepart())),
      (operateur, periode) -> Optional.of(LE_LUNDI_11_MAI_2026_A_16H),
      LE_MARDI_12_MAI_2026_A_10H
    ).historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true)
    );
  }

  @Test
  void shouldLaisserOuverteSurSonJourUneJourneeEncoreSousLeSeuil() {
    FeuilleDeTemps feuille = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe7HSansDepart())),
      (operateur, periode) -> {
        throw new AssertionError("aucun pointage ne doit etre cherche pour une journee en cours");
      },
      LE_LUNDI_11_MAI_2026_A_20H
    ).historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026)).last().isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.empty()));
  }

  /**
   * Issue #59 : une journee fermee de plus de 24 h n'est comptee que jusqu'a sa fin presumee, la fin de l'OF a 16:00.
   * Mardi et mercredi ne portent rien.
   */
  @Test
  void shouldBornerASaFinPresumeeUneJourneeFermeeDePlusDe24H() {
    FeuilleDeTemps feuille = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundi7HAuMercredi8H())),
      (operateur, periode) -> Optional.of(LE_LUNDI_11_MAI_2026_A_16H),
      LE_MERCREDI_13_MAI_2026_A_8H
    ).historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true)
    );
    assertThat(presenceDu(feuille, MARDI_12_MAI_2026)).isEmpty();
    assertThat(presenceDu(feuille, MERCREDI_13_MAI_2026)).isEmpty();
  }

  /**
   * E7 : le poste de nuit du dimanche au lundi se lit sur deux semaines, coupe a minuit a Paris.
   */
  @Test
  void shouldDecouperUnPosteDeNuitSurDeuxSemaines() {
    PresencesEnMemoire presences = PresencesEnMemoire.avec(List.of(journeeDuDimanche20HAuLundi8H()));
    FeuillesDeTempsService service = service(presences, AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H);

    assertThat(presenceDu(service.historique(OPERATEUR_ID_DUPONT, SEMAINE_19_DE_2026), DIMANCHE_10_MAI_2026)).containsExactly(
      new Plage(LE_DIMANCHE_10_MAI_2026_A_20H, Optional.of(LE_LUNDI_11_MAI_2026_A_MINUIT))
    );
    assertThat(presenceDu(service.historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026), LUNDI_11_MAI_2026)).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_MINUIT, Optional.of(LE_LUNDI_11_MAI_2026_A_8H))
    );
  }

  @Test
  void shouldDemanderLeTravailDepuisLeLundiQuandAucuneJourneeNeLePrecede() {
    TravailEnMemoire travail = TravailEnMemoire.sansSuivi();

    historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())), travail);

    assertThat(travail.depuisDemande()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(travail.finExclusiveDemandee()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  /**
   * Un poste de nuit arrive dimanche soir a pu demarrer son travail avant minuit : les suivis se cherchent depuis son
   * arrivee.
   */
  @Test
  void shouldDemanderLeTravailDepuisLaPremiereArriveeQuandElleEstAnterieureAuLundi() {
    TravailEnMemoire travail = TravailEnMemoire.sansSuivi();

    historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H(), journeeDuDimanche20HAuLundi8H())), travail);

    assertThat(travail.depuisDemande()).isEqualTo(LE_DIMANCHE_10_MAI_2026_A_20H);
  }

  @Test
  void shouldNeRendreAucuneActiviteSansTravail() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())));

    assertThat(feuille.jours()).allSatisfy(jour -> assertThat(jour.activites()).isEmpty());
  }

  /**
   * Une fin a midi coupe le travail, un debut le relance : deux activites autour de la coupure.
   */
  @Test
  void shouldRattacherLeTravailAuJourQuiLePorte() {
    FeuilleDeTemps feuille = historiqueDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(
          suiviDuCarter(
            debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H),
            finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H),
            debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_13H),
            finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_17H)
          )
        )
      )
    );

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      travailDuCarter(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)),
      travailDuCarter(LE_LUNDI_11_MAI_2026_A_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))
    );
    assertThat(activitesDu(feuille, MARDI_12_MAI_2026)).isEmpty();
  }

  @Test
  void shouldRefermerAuDepartUnTravailJamaisArrete() {
    FeuilleDeTemps feuille = historiqueDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_9H))))
    );

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      travailDuCarter(LE_LUNDI_11_MAI_2026_A_9H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))
    );
  }

  @Test
  void shouldLaisserOuvertSurSonJourUnTravailEnCours() {
    FeuilleDeTemps feuille = historiqueDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuMardiOuverteA8H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_MARDI_12_MAI_2026_A_9H))))
    );

    assertThat(activitesDu(feuille, MARDI_12_MAI_2026)).containsExactly(travailDuCarter(LE_MARDI_12_MAI_2026_A_9H, Optional.empty()));
    assertThat(activitesDu(feuille, MERCREDI_13_MAI_2026)).isEmpty();
  }

  @Test
  void shouldEcarterUnTravailCommenceHorsDeTouteJournee() {
    FeuilleDeTemps feuille = historiqueDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_18H))))
    );

    assertThat(feuille.jours()).allSatisfy(jour -> assertThat(jour.activites()).isEmpty());
  }

  /**
   * Un poste de nuit se coupe a minuit, sur chacun des deux jours.
   */
  @Test
  void shouldScinderAMinuitLeTravailDUnPosteDeNuit() {
    FeuilleDeTemps feuille = historiqueDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundi22HAuMardi2H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_23H), finSurLaDmu50A(LE_MARDI_12_MAI_2026_A_1H))))
    );

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      travailDuCarter(LE_LUNDI_11_MAI_2026_A_23H, Optional.of(LE_MARDI_12_MAI_2026_A_MINUIT))
    );
    assertThat(activitesDu(feuille, MARDI_12_MAI_2026)).containsExactly(
      travailDuCarter(LE_MARDI_12_MAI_2026_A_MINUIT, Optional.of(LE_MARDI_12_MAI_2026_A_1H))
    );
  }

  @Test
  void shouldPresumerLeTravailDUneJourneeAbandonnee() {
    FeuilleDeTemps feuille = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe7HSansDepart())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H)))),
      (operateur, periode) -> Optional.of(LE_LUNDI_11_MAI_2026_A_16H),
      LE_MARDI_12_MAI_2026_A_10H
    ).historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new IntervalleDActivite(
        activiteDeTravailDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true)
      )
    );
  }

  /**
   * Deux elements en meme temps : deux activites qui se chevauchent, dans l'ordre de leurs debuts, quel que soit
   * l'ordre des suivis rendus par le port.
   */
  @Test
  void shouldTrierLesActivitesDUnJourParDebut() {
    SuiviDuTravail bride = new SuiviDuTravail(
      ELEMENT_ID_BRIDE,
      new JournalDAtelier(List.of(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H))),
      Optional.empty()
    );

    FeuilleDeTemps feuille = historiqueDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(bride, suiviDuCarter(debutAuTourA(LE_LUNDI_11_MAI_2026_A_9H))))
    );

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new IntervalleDActivite(
        activiteDeTravailDuCarterAuTour(),
        new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))
      ),
      new IntervalleDActivite(
        activiteDeTravailDeLaBrideSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_10H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))
      )
    );
  }

  /**
   * Deux debuts au meme instant se departagent par l'element, pour que deux lectures rendent le meme ordre.
   */
  @Test
  void shouldDepartagerParLElementDeuxActivitesCommenceesEnsemble() {
    SuiviDuTravail bride = new SuiviDuTravail(
      ELEMENT_ID_BRIDE,
      new JournalDAtelier(List.of(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_9H))),
      Optional.empty()
    );

    FeuilleDeTemps feuille = historiqueDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(bride, suiviDuCarter(debutAuTourA(LE_LUNDI_11_MAI_2026_A_9H))))
    );

    assertThat(activitesDu(feuille, LUNDI_11_MAI_2026))
      .extracting(intervalle -> intervalle.activite().element())
      .containsExactly(ELEMENT_ID_CARTER, ELEMENT_ID_BRIDE);
  }

  private static SuiviDuTravail suiviDuCarter(PointageDAtelier... pointages) {
    return new SuiviDuTravail(ELEMENT_ID_CARTER, new JournalDAtelier(List.of(pointages)), Optional.empty());
  }

  private static IntervalleDActivite travailDuCarter(Instant debut, Optional<Instant> fin) {
    return new IntervalleDActivite(activiteDeTravailDuCarterSurLaDmu50(), new Plage(debut, fin));
  }

  private static FeuilleDeTemps historiqueDeDupont(PresencesEnMemoire presences) {
    return historiqueDeDupont(presences, TravailEnMemoire.sansSuivi());
  }

  private static FeuilleDeTemps historiqueDeDupont(PresencesEnMemoire presences, TravailEnMemoire travail) {
    return service(presences, travail, AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H).historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
  }

  private static FeuillesDeTempsService service(PresencesEnMemoire presences, PointagesDAtelier pointages, Instant maintenant) {
    return service(presences, TravailEnMemoire.sansSuivi(), pointages, maintenant);
  }

  private static FeuillesDeTempsService service(
    PresencesEnMemoire presences,
    TravailEnMemoire travail,
    PointagesDAtelier pointages,
    Instant maintenant
  ) {
    return FeuillesDeTempsService.builder()
      .presences(presences)
      .operateurs(REFERENTIEL)
      .fuseau(A_PARIS)
      .seuil(() -> AMPLITUDE_MAXIMALE_13H)
      .pointages(pointages)
      .travail(travail)
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

  private static List<Plage> presenceDu(FeuilleDeTemps feuille, LocalDate jour) {
    return feuille
      .jours()
      .stream()
      .filter(jourDeLaSemaine -> jourDeLaSemaine.jour().equals(jour))
      .findFirst()
      .orElseThrow()
      .presence();
  }
}

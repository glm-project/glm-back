package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class JournalDAtelierTest {

  @Test
  void shouldNotBuildWithoutEvenements() {
    assertThatThrownBy(() -> new JournalDAtelier(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evenements");
  }

  @Test
  void shouldNotBuildWithNullEvenement() {
    List<EvenementDAtelier> avecNull = Arrays.asList(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), null);

    assertThatThrownBy(() -> new JournalDAtelier(avecNull))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("evenements");
  }

  @Test
  void shouldBuildJournalVide() {
    JournalDAtelier journal = JournalDAtelier.vide();

    assertThat(journal.evenements()).isEmpty();
    assertThat(journal.actifs()).isEmpty();
    assertThat(journal.intervalles(Optional.empty())).isEmpty();
  }

  @Test
  void shouldTrierLesEvenementsParDateDeSurvenue() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);

    JournalDAtelier journal = new JournalDAtelier(List.of(fin, debut));

    assertThat(journal.evenements()).containsExactly(debut, fin);
  }

  /**
   * A heure egale, la fin passe avant l'ouverture : ni sa saisie, enregistree le lendemain, ni son identifiant ne la
   * font passer apres. Le journal ne depend jamais de l'ordre de reception.
   */
  @Test
  void shouldRangerLaFinAvantLOuvertureSimultaneeQuellesQueSoientSaSaisieEtSonIdentifiant() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier relance = ouvertureNumero(1, LE_10_MAI_2026_A_12H);
    EvenementDAtelier finRegularisee = finRegulariseeParLeroyDe(debut).a(LE_10_MAI_2026_A_12H);

    JournalDAtelier journal = new JournalDAtelier(List.of(relance, finRegularisee, debut));

    assertThat(journal.evenements()).containsExactly(debut, finRegularisee, relance);
  }

  @Test
  void shouldRangerLaTransitionAvantLOuvertureSimultanee() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier relance = ouvertureNumero(1, LE_10_MAI_2026_A_12H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(debut).a(LE_10_MAI_2026_A_12H);

    JournalDAtelier journal = new JournalDAtelier(List.of(relance, nonConformite, debut));

    assertThat(journal.evenements()).containsExactly(debut, nonConformite, relance);
  }

  @Test
  void shouldDepartagerLesSaisiesIdentiquesParId() {
    EvenementDAtelier premier = ouvertureNumero(1, LE_10_MAI_2026_A_8H);
    EvenementDAtelier second = ouvertureNumero(2, LE_10_MAI_2026_A_8H);

    JournalDAtelier journal = new JournalDAtelier(List.of(second, premier));

    assertThat(journal.evenements()).containsExactly(premier, second);
  }

  @Test
  void shouldRefuserUneFinQuiNeViseAucuneActiviteEnCours() {
    EvenementDAtelier jamaisEnregistre = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    List<EvenementDAtelier> finSansDebut = List.of(finDe(jamaisEnregistre).a(LE_10_MAI_2026_A_12H));

    assertThatThrownBy(() -> new JournalDAtelier(finSansDebut))
      .isExactlyInstanceOf(TransitionDAtelierInterditeException.class)
      .hasMessageContaining("FIN")
      .hasMessageContaining(OPERATEUR_ID_DUPONT.uuid().toString())
      .hasMessageContaining(jamaisEnregistre.id().uuid().toString());
  }

  @Test
  void shouldJouerLesActivitesIndependammentPourChaqueOperateur() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::operateur)
      .containsExactly(OPERATEUR_ID_DUPONT, OPERATEUR_ID_MARTIN);
  }

  /**
   * Le cas de l'erosionniste : deux pieces du meme element sur deux machines. Sans le poste dans la cle, le second
   * debut terminerait le premier.
   */
  @Test
  void shouldJouerLesActivitesIndependammentPourChaquePosteDUnMemeOperateur() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::poste, IntervalleDActivite::fin)
      .containsExactly(
        tuple(Optional.of(POSTE_ID_FRAISEUSE_1), Optional.empty()),
        tuple(Optional.of(POSTE_ID_FRAISEUSE_2), Optional.empty())
      );
  }

  @Test
  void shouldJouerUneActiviteUniqueQuandAucunPosteNEstRenseigne() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSansPosteParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.intervalles(Optional.empty()))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.poste()).isEmpty());
  }

  @Test
  void shouldFermerUnIntervalleSurLaFinDeSonActivite() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_12H)));

    assertThat(journal.intervalles(Optional.empty()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.evenement()).isEqualTo(debut.id());
        assertThat(intervalle.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_12H);
      });
  }

  @Test
  void shouldLaisserOuvertLeDernierIntervalleDUneActivite() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.intervalles(Optional.empty())).singleElement().matches(IntervalleDActivite::estOuvert);
  }

  @Test
  void shouldFermerLeDernierIntervalleSurLaFermetureFinale() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.intervalles(Optional.of(LE_10_MAI_2026_A_17H)))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldNOuvrirAucunIntervalleSurUneFin() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_12H)));

    assertThat(journal.intervalles(Optional.of(LE_10_MAI_2026_A_17H))).hasSize(1);
  }

  @Test
  void shouldCategoriserChaqueActiviteSurLePointageQuiLOuvre() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::categorie)
      .containsExactly(CategorieDActivite.NON_CONFORMITE, CategorieDActivite.TRAVAIL);
  }

  /**
   * Travail, non conformite puis travail : chaque transition termine l'activite qu'elle vise et en ouvre une
   * distincte, sans trou ni recouvrement.
   */
  @Test
  void shouldEnchainerTravailNonConformitePuisTravailEnActivitesDistinctes() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_9H);
    EvenementDAtelier reprise = passageEnTravailDe(nonConformite).a(LE_10_MAI_2026_A_12H);

    JournalDAtelier journal = new JournalDAtelier(List.of(travail, nonConformite, reprise, finDe(reprise).a(LE_10_MAI_2026_A_13H)));

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::evenement, IntervalleDActivite::categorie, IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(travail.id(), CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_9H)),
        tuple(nonConformite.id(), CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(reprise.id(), CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_12H, Optional.of(LE_10_MAI_2026_A_13H))
      );
  }

  /**
   * Une transition de meme categorie contredit l'activite qu'elle vise : elle n'est pas une relance deguisee.
   */
  @Test
  void shouldRefuserUneTransitionVersLaMemeCategorie() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    List<EvenementDAtelier> travailSurTravail = List.of(travail, passageEnTravailDe(travail).a(LE_10_MAI_2026_A_9H));

    assertThatThrownBy(() -> new JournalDAtelier(travailSurTravail))
      .isExactlyInstanceOf(TransitionDAtelierInterditeException.class)
      .hasMessageContaining("dans l'autre categorie");
  }

  /**
   * Une transition dont la cible est deja terminee reste une transition : elle n'ouvre pas implicitement une activite.
   */
  @Test
  void shouldRefuserUneTransitionQuiViseUneActiviteDejaTerminee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_9H)));
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> journal.enregistre(nonConformite)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  /**
   * A a 8 h, relance B a 9 h, puis une fin qui vise A : elle ne termine jamais B.
   */
  @Test
  void shouldNeJamaisTerminerLaRemplacanteDeLActiviteVisee() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(a, debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H)));
    EvenementDAtelier finDeA = finDe(a).a(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> journal.enregistre(finDeA)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  /**
   * Travail a 8 h, transition vers une non conformite a 12 h et fin du travail a 17 h se contredisent, dans un ordre
   * de reception comme dans l'autre : la fin ne termine jamais la non conformite.
   */
  @Test
  void shouldRefuserUneFinQuiViseUneActiviteDejaRemplaceeQuelQueSoitLOrdreDeReception() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_17H);
    JournalDAtelier finRecueEnPremier = new JournalDAtelier(List.of(travail, finDuTravail));
    JournalDAtelier transitionRecueEnPremier = new JournalDAtelier(List.of(travail, nonConformite));

    assertThatThrownBy(() -> finRecueEnPremier.enregistre(nonConformite)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
    assertThatThrownBy(() -> transitionRecueEnPremier.enregistre(finDuTravail)).isExactlyInstanceOf(
      TransitionDAtelierInterditeException.class
    );
  }

  @Test
  void shouldRefuserUnGesteQuiViseUneActiviteIntrouvableDansLeJournal() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    EvenementDAtelier ailleurs = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(ailleurs).a(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> journal.enregistre(fin))
      .isExactlyInstanceOf(ActiviteViseeIntrouvableException.class)
      .hasMessageContaining(ailleurs.id().uuid().toString());
  }

  @Test
  void shouldRefuserUnGesteQuiViseLActiviteDUnAutrePoste() {
    EvenementDAtelier surFraiseuse1 = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(surFraiseuse1));
    EvenementDAtelier finSurFraiseuse2 = finSurFraiseuse2Visant(surFraiseuse1, LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> journal.exigeLActiviteViseePar(finSurFraiseuse2))
      .isExactlyInstanceOf(ActiviteViseeIncoherenteException.class)
      .hasMessageContaining(surFraiseuse1.id().uuid().toString());
  }

  /**
   * Une activite dont l'ouvrant est annule reste une activite de ce journal : le geste qui la vise n'est pas
   * introuvable, il contredit le journal.
   */
  @Test
  void shouldRefuserSansLaDireIntrouvableUneFinQuiViseUnOuvrantAnnule() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut)).annule(debut.id(), annulationParLeroy());
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> journal.enregistre(fin)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  @Test
  void shouldEcarterDuRepliLesEvenementsAnnules() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut));

    JournalDAtelier corrige = journal.annule(debut.id(), annulationParLeroy());

    assertThat(corrige.evenements()).hasSize(1);
    assertThat(corrige.actifs()).isEmpty();
    assertThat(corrige.intervalles(Optional.empty())).isEmpty();
  }

  @Test
  void shouldConserverLaTraceDeLEvenementAnnule() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    JournalDAtelier corrige = new JournalDAtelier(List.of(debut)).annule(debut.id(), annulationParLeroy());

    assertThat(corrige.evenement(debut.id()))
      .isPresent()
      .get()
      .satisfies(evenement -> {
        assertThat(evenement.estAnnule()).isTrue();
        assertThat(evenement.annulation()).contains(annulationParLeroy());
      });
  }

  @Test
  void shouldNotAnnulerUnEvenementInconnu() {
    JournalDAtelier journal = JournalDAtelier.vide();
    EvenementDAtelierId inconnu = EvenementDAtelierId.newId();
    Annulation annulation = annulationParLeroy();

    assertThatThrownBy(() -> journal.annule(inconnu, annulation))
      .isExactlyInstanceOf(EvenementDAtelierIntrouvableException.class)
      .hasMessageContaining("introuvable");
  }

  @Test
  void shouldRefuserUneAnnulationQuiLaisseraitUneFinOrpheline() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_12H)));
    Annulation annulation = annulationParLeroy();

    assertThatThrownBy(() -> journal.annule(debut.id(), annulation)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  /**
   * Le debut corrige garde son activite : la fin qui la visait la termine toujours, a l'heure du debut corrige.
   */
  @Test
  void shouldCorrigerUnEvenementEnUnSeulActe() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_12H)));
    EvenementDAtelier remplacant = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_7H30);

    JournalDAtelier corrige = journal.corrige(debut.id(), annulationParLeroy(), remplacant);

    assertThat(corrige.intervalles(Optional.empty()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.evenement()).isEqualTo(remplacant.id());
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_7H30);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_12H);
      });
    assertThat(corrige.evenement(remplacant.id()))
      .get()
      .satisfies(enPlace -> assertThat(enPlace.activite()).isEqualTo(debut.activite()));
  }

  /**
   * Apres correction de son ouvrant, un geste visant l'activite se resout sur l'ouvrant actif : le remplacant.
   */
  @Test
  void shouldResoudreLaCibleSurLOuvrantActifApresCorrection() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier corrige = new JournalDAtelier(List.of(debut)).corrige(
      debut.id(),
      annulationParLeroy(),
      debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_7H30)
    );

    JournalDAtelier termine = corrige.enregistre(finDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(termine.intervalles(Optional.empty()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_7H30);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_12H);
      });
  }

  @Test
  void shouldRefuserUneCorrectionQuiViseUneActiviteIntrouvable() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, fin));
    EvenementDAtelierId corrigee = fin.id();
    Annulation annulation = annulationParLeroy();
    EvenementDAtelier finAilleurs = finRegulariseeParLeroyDe(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)).a(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> journal.corrige(corrigee, annulation, finAilleurs)).isExactlyInstanceOf(
      ActiviteViseeIntrouvableException.class
    );
  }

  @Test
  void shouldNotCorrigerUnEvenementInconnu() {
    JournalDAtelier journal = JournalDAtelier.vide();
    EvenementDAtelierId inconnu = EvenementDAtelierId.newId();
    Annulation annulation = annulationParLeroy();
    EvenementDAtelier remplacant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> journal.corrige(inconnu, annulation, remplacant)).isExactlyInstanceOf(
      EvenementDAtelierIntrouvableException.class
    );
  }

  /**
   * Une transition rattrapee apres coup referme l'activite qu'elle vise a son heure ; la relance deja pointee referme
   * la non conformite qu'elle ouvre.
   */
  @Test
  void shouldRefermerLIntervallePrecedentSurUneInsertionRetroactive() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H)));

    JournalDAtelier regularise = journal.enregistre(passageEnNonConformiteDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(regularise.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::categorie, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, CategorieDActivite.TRAVAIL, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(LE_10_MAI_2026_A_12H, CategorieDActivite.NON_CONFORMITE, Optional.of(LE_10_MAI_2026_A_13H)),
        tuple(LE_10_MAI_2026_A_13H, CategorieDActivite.TRAVAIL, Optional.empty())
      );
  }

  /**
   * Un operateur qui revient sur un element reste ouvert n'est jamais bloque : son ouverture termine l'activite en
   * cours et en ouvre une nouvelle, sans trou ni recouvrement.
   */
  @Test
  void shouldRelancerUneActiviteDejaEnCours() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::categorie, IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_13H)),
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_13H, Optional.empty())
      );
  }

  @Test
  void shouldRelancerUneNonConformiteDejaEnCours() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::categorie, IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_13H)),
        tuple(CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_13H, Optional.empty())
      );
  }

  /**
   * Le double appui : deux ouvertures a la meme seconde. La premiere ne dure rien, le total reste celui d'une seule.
   */
  @Test
  void shouldNeRienAjouterSurUnDoubleAppuiSimultane() {
    EvenementDAtelier secondAppui = ouvertureNumero(2, LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(
      List.of(ouvertureNumero(1, LE_10_MAI_2026_A_8H), secondAppui, finDe(secondAppui).a(LE_10_MAI_2026_A_12H))
    );

    List<IntervalleDActivite> intervalles = journal.intervalles(Optional.empty());

    assertThat(intervalles)
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_8H)),
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H))
      );
    assertThat(duree(intervalles)).isEqualTo(Duration.ofHours(4));
  }

  @Test
  void shouldNeRienAjouterSurUnDoubleAppuiDecale() {
    EvenementDAtelier secondAppui = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H.plusSeconds(3));
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), secondAppui, finDe(secondAppui).a(LE_10_MAI_2026_A_12H))
    );

    assertThat(duree(journal.intervalles(Optional.empty()))).isEqualTo(Duration.ofHours(4));
  }

  @Test
  void shouldArreterUneActiviteRelancee() {
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), relance, finDe(relance).a(LE_10_MAI_2026_A_17H))
    );

    assertThat(journal.intervalles(Optional.of(LE_11_MAI_2026_A_9H15)))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_13H)),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  /**
   * Un debut regularise au milieu d'une activite deja terminee la remplace a son heure : la fin pointee plus tard vise
   * alors une activite deja remplacee, et se contredit avec lui.
   */
  @Test
  void shouldRefuserUnDebutRegulariseAuMilieuDUneActiviteDejaTerminee() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_17H)));
    EvenementDAtelier debutRegularise = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> journal.enregistre(debutRegularise)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  @Test
  void shouldAnnulerLaRelanceDUneActivite() {
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), relance));

    JournalDAtelier corrige = journal.annule(relance.id(), annulationParLeroy());

    assertThat(corrige.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, Optional.empty()));
  }

  /**
   * Annuler le premier debut d'une relance ne laisse aucune fin orpheline : la fin vise la relance.
   */
  @Test
  void shouldAnnulerLePremierDebutDUneActiviteRelancee() {
    EvenementDAtelier premier = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    JournalDAtelier journal = new JournalDAtelier(List.of(premier, relance, finDe(relance).a(LE_10_MAI_2026_A_17H)));

    JournalDAtelier corrige = journal.annule(premier.id(), annulationParLeroy());

    assertThat(corrige.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H)));
  }

  /**
   * La non conformite corrigee en relance du travail garde son activite : la fin qui la visait la termine toujours.
   */
  @Test
  void shouldCorrigerUneNonConformiteEnRelance() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(debut).a(LE_10_MAI_2026_A_12H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, nonConformite, finDe(nonConformite).a(LE_10_MAI_2026_A_17H)));

    JournalDAtelier corrige = journal.corrige(
      nonConformite.id(),
      annulationParLeroy(),
      debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_12H)
    );

    assertThat(corrige.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::categorie, IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_12H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  @Test
  void shouldNotReadEvenementInconnu() {
    assertThat(JournalDAtelier.vide().evenement(EvenementDAtelierId.newId())).isEmpty();
  }

  private static Duration duree(List<IntervalleDActivite> intervalles) {
    return intervalles
      .stream()
      .map(intervalle -> Duration.between(intervalle.debut(), intervalle.fin().orElseThrow()))
      .reduce(Duration.ZERO, Duration::plus);
  }

  /**
   * Une ouverture de Dupont sur la fraiseuse 1 dont l'identifiant est fixe : de quoi prouver qu'il ne departage que
   * des gestes simultanes de meme intention.
   */
  private static EvenementDAtelier ouvertureNumero(long numero, Instant date) {
    EvenementDAtelierId id = new EvenementDAtelierId(new UUID(0, numero));

    return EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(Horodatage.saisiA(date));
  }

  private static EvenementDAtelier finSurFraiseuse2Visant(EvenementDAtelier ouvrant, Instant date) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activite(Optional.empty())
      .activiteVisee(ouvrant.activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_2))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(Horodatage.saisiA(date));
  }
}

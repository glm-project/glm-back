package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    assertThat(journal.activites(Optional.empty())).isEmpty();
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

  /**
   * Relu tel quel, un journal ne se refuse jamais : une fin dont la cible n'y est pas ouverte laisse une sequence en
   * conflit sans activite, jamais une exception qui bloquerait la relecture.
   */
  @Test
  void shouldConserverEnConflitUneFinQuiNeViseAucuneActiviteDuJournal() {
    EvenementDAtelier jamaisEnregistre = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier finSansDebut = finDe(jamaisEnregistre).a(LE_10_MAI_2026_A_12H);

    JournalDAtelier journal = new JournalDAtelier(List.of(finSansDebut));

    assertThat(journal.activites(Optional.empty())).isEmpty();
    assertThat(journal.conflits(Optional.empty()))
      .singleElement()
      .satisfies(conflit -> {
        assertThat(conflit.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
        assertThat(conflit.activites()).isEmpty();
        assertThat(conflit.pointages()).containsExactly(finSansDebut.id());
      });
  }

  @Test
  void shouldJouerLesActivitesIndependammentPourChaqueOperateur() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H))
    );

    assertThat(journal.activites(Optional.empty()))
      .extracting(activite -> activite.cle().operateur())
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

    assertThat(journal.activites(Optional.empty()))
      .extracting(activite -> activite.cle().poste(), Activite::fin)
      .containsExactly(
        tuple(Optional.of(POSTE_ID_FRAISEUSE_1), Optional.empty()),
        tuple(Optional.of(POSTE_ID_FRAISEUSE_2), Optional.empty())
      );
  }

  @Test
  void shouldJouerUneActiviteUniqueQuandAucunPosteNEstRenseigne() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSansPosteParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.activites(Optional.empty()))
      .singleElement()
      .satisfies(activite -> assertThat(activite.cle().poste()).isEmpty());
  }

  @Test
  void shouldFermerUnIntervalleSurLaFinDeSonActivite() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_12H)));

    assertThat(journal.activites(Optional.empty()))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.ouvrant()).isEqualTo(debut);
        assertThat(activite.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
        assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_12H);
      });
  }

  @Test
  void shouldLaisserOuvertLeDernierIntervalleDUneActivite() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.activites(Optional.empty()))
      .singleElement()
      .matches(activite -> activite.fin().isEmpty());
  }

  @Test
  void shouldFermerLeDernierIntervalleSurLaFermetureFinale() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.activites(Optional.of(LE_10_MAI_2026_A_17H)))
      .singleElement()
      .satisfies(activite -> assertThat(activite.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldNOuvrirAucunIntervalleSurUneFin() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_12H)));

    assertThat(journal.activites(Optional.of(LE_10_MAI_2026_A_17H))).hasSize(1);
  }

  @Test
  void shouldCategoriserChaqueActiviteSurLePointageQuiLOuvre() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    assertThat(journal.activites(Optional.empty()))
      .extracting(Activite::categorie)
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

    assertThat(journal.activites(Optional.empty()))
      .extracting(Activite::ouvrant, Activite::categorie, Activite::debut, Activite::fin)
      .containsExactly(
        tuple(travail, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_9H)),
        tuple(nonConformite, CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(reprise, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_12H, Optional.of(LE_10_MAI_2026_A_13H))
      );
  }

  /**
   * Une transition de meme categorie n'est pas une relance deguisee : elle est conservee, et la sequence est en
   * conflit.
   */
  @Test
  void shouldConserverEnConflitUneTransitionVersLaMemeCategorie() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier travailSurTravail = passageEnTravailDe(travail).a(LE_10_MAI_2026_A_9H);

    JournalDAtelier journal = new JournalDAtelier(List.of(travail)).enregistre(travailSurTravail);

    assertThat(journal.actifs()).containsExactly(travail, travailSurTravail);
    assertThat(journal.conflits(Optional.empty())).hasSize(1);
  }

  /**
   * Une transition dont la cible est deja terminee reste une transition : conservee, elle n'ouvre pas implicitement
   * une activite, elle met la sequence en conflit.
   */
  @Test
  void shouldConserverEnConflitUneTransitionQuiViseUneActiviteDejaTerminee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_9H)));

    JournalDAtelier conserve = journal.enregistre(passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThat(conserve.actifs()).hasSize(3);
    assertThat(conserve.activites(Optional.empty())).allSatisfy(activite -> assertThat(activite.aResoudre()).isTrue());
    assertThat(conserve.conflits(Optional.empty())).hasSize(1);
  }

  /**
   * A a 8 h, relance B a 9 h, puis une fin qui vise A : elle est conservee, et ne termine jamais B.
   */
  @Test
  void shouldNeJamaisTerminerLaRemplacanteDeLActiviteVisee() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    JournalDAtelier journal = new JournalDAtelier(List.of(a, b));

    JournalDAtelier conserve = journal.enregistre(finDe(a).a(LE_10_MAI_2026_A_12H));

    assertThat(conserve.activites(Optional.empty()))
      .extracting(Activite::ouvrant, Activite::fin, Activite::aResoudre)
      .containsExactly(tuple(a, Optional.empty(), true), tuple(b, Optional.empty(), true));
  }

  /**
   * La table du plan, recue dans tous les ordres : travail A a 08 h, transition A -> NC a 12 h, fin de A a 17 h. Un
   * fait deja accepte devient contradictoire a l'arrivee d'un fait anterieur, et la sequence en conflit est la meme
   * quel que soit l'ordre de reception.
   */
  @Test
  void shouldRendreLaMemeSequenceEnConflitQuelQueSoitLOrdreDeReception() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_17H);

    assertThat(interpretationsSelonLOrdreDeReception(List.of(travail, nonConformite, finDuTravail))).containsOnly(
      new Interpretation(
        List.of(
          new Activite(travail, Optional.empty(), Optional.of(Instant.parse("2026-05-10T21:00:00Z"))),
          new Activite(nonConformite, Optional.empty(), Optional.of(Instant.parse("2026-05-11T01:00:00Z")))
        ),
        List.of(
          new SequenceEnConflit(
            cleDeFraiseuse1DeDupont(),
            List.of(travail.activite().orElseThrow(), nonConformite.activite().orElseThrow()),
            List.of(travail.id(), nonConformite.id(), finDuTravail.id())
          )
        )
      )
    );
  }

  @Test
  void shouldBornerLaPlagePossibleDuConflitParLesEcheances() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    JournalDAtelier journal = new JournalDAtelier(List.of(travail, nonConformite, finDe(travail).a(LE_10_MAI_2026_A_17H)));

    assertThat(journal.activites(Optional.empty()))
      .extracting(Activite::finAuPlusTard)
      .containsExactly(Optional.of(Instant.parse("2026-05-10T21:00:00Z")), Optional.of(Instant.parse("2026-05-11T01:00:00Z")));
  }

  @Test
  void shouldEtendreLaPlagePossibleJusquaLaRegularisationRecevable() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    Instant finRegularisee = Instant.parse("2026-05-10T23:00:00Z");
    JournalDAtelier journal = new JournalDAtelier(List.of(travail, nonConformite, finRegulariseeParLeroyDe(travail).a(finRegularisee)));

    assertThat(journal.activites(Optional.empty()).getFirst().finAuPlusTard()).contains(finRegularisee);
  }

  @Test
  void shouldLimiterLaPlagePossibleParLaClotureSansLaProlonger() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    JournalDAtelier journal = new JournalDAtelier(List.of(travail, nonConformite, finDe(travail).a(LE_10_MAI_2026_A_17H)));

    assertThat(journal.activites(Optional.of(LE_10_MAI_2026_A_17H)))
      .extracting(Activite::finAuPlusTard)
      .containsExactly(Optional.of(LE_10_MAI_2026_A_17H), Optional.of(LE_10_MAI_2026_A_17H));
    assertThat(journal.activites(Optional.of(LE_11_MAI_2026_A_9H15)))
      .extracting(Activite::finAuPlusTard)
      .containsExactly(Optional.of(Instant.parse("2026-05-10T21:00:00Z")), Optional.of(Instant.parse("2026-05-11T01:00:00Z")));
  }

  /**
   * Les autres exemples de conflit, recus dans tous les ordres : chacun rend une seule interpretation, qu'un fait
   * anterieur arrive apres coup ou non.
   */
  @Test
  void shouldInterpreterChaqueExempleDeConflitIndependammentDeLOrdreDeReception() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    EvenementDAtelier echueB = debutSurFraiseuse1ParDupontA(Instant.parse("2026-05-10T22:00:00Z"));
    EvenementDAtelier c = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    List<List<EvenementDAtelier>> exemples = List.of(
      List.of(a, b, finDe(a).a(LE_10_MAI_2026_A_12H)),
      List.of(a, b, finDe(a).a(LE_10_MAI_2026_A_12H), c),
      List.of(a, finDe(a).a(LE_10_MAI_2026_A_12H), finDe(a).a(LE_10_MAI_2026_A_12H.plusSeconds(2))),
      List.of(a, finDe(a).a(LE_10_MAI_2026_A_12H), passageEnNonConformiteDe(a).a(LE_10_MAI_2026_A_13H)),
      List.of(a, passageEnTravailDe(a).a(LE_10_MAI_2026_A_12H), finDe(a).a(LE_10_MAI_2026_A_13H)),
      List.of(a, echueB, passageEnNonConformiteDe(a).a(Instant.parse("2026-05-10T23:00:00Z"))),
      List.of(a, echueB, finRegulariseeParLeroyDe(a).a(Instant.parse("2026-05-10T23:00:00Z")))
    );

    exemples.forEach(faits ->
      assertThat(interpretationsSelonLOrdreDeReception(faits))
        .describedAs("%s", faits)
        .singleElement()
        .satisfies(interpretation -> assertThat(interpretation.conflits()).isNotEmpty())
    );
  }

  /**
   * Un geste recu avant l'ouverture qu'il vise est refuse : l'activite est introuvable dans le journal.
   */
  @Test
  void shouldRefuserUnGesteRecuAvantLOuvertureQuIlVise() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    JournalDAtelier vide = JournalDAtelier.vide();

    assertThatThrownBy(() -> vide.enregistre(fin)).isExactlyInstanceOf(ActiviteViseeIntrouvableException.class);
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
   * introuvable. Il est conserve, et la sequence est en conflit.
   */
  @Test
  void shouldConserverEnConflitUneFinQuiViseUnOuvrantAnnule() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut)).annule(debut.id(), annulationParLeroy());
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);

    JournalDAtelier conserve = journal.enregistre(fin);

    assertThat(conserve.actifs()).containsExactly(fin);
    assertThat(conserve.conflits(Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(fin.id()));
  }

  @Test
  void shouldEcarterDuRepliLesEvenementsAnnules() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut));

    JournalDAtelier corrige = journal.annule(debut.id(), annulationParLeroy());

    assertThat(corrige.evenements()).hasSize(1);
    assertThat(corrige.actifs()).isEmpty();
    assertThat(corrige.activites(Optional.empty())).isEmpty();
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

  /**
   * Annuler une ouverture que vise une fin est admis : la fin reste, et la sequence est en conflit plutot que refusee.
   */
  @Test
  void shouldConserverEnConflitLAnnulationDUneOuvertureVisee() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, fin));

    JournalDAtelier annule = journal.annule(debut.id(), annulationParLeroy());

    assertThat(annule.activites(Optional.empty())).isEmpty();
    assertThat(annule.conflits(Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(fin.id()));
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

    assertThat(corrige.activites(Optional.empty()))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.ouvrant().id()).isEqualTo(remplacant.id());
        assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_7H30);
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_12H);
      });
    assertThat(corrige.evenement(remplacant.id()))
      .get()
      .satisfies(enPlace -> assertThat(enPlace.activite()).isEqualTo(debut.activite()));
  }

  @Test
  void shouldRelierUneCorrectionALOrigineDeSonOuvrant() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier remplacant = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_7H30);

    JournalDAtelier corrige = new JournalDAtelier(List.of(debut)).corrige(debut.id(), annulationParLeroy(), remplacant);

    assertThat(corrige.evenement(remplacant.id()).orElseThrow().remplace()).contains(debut.id());
  }

  @Test
  void shouldRelierLaCorrectionDUneFinEtConserverSaChaineALAnnulation() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier remplacant = finDe(debut).a(LE_10_MAI_2026_A_12H.plusSeconds(1800));
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, fin));

    JournalDAtelier corrige = journal.corrige(fin.id(), annulationParLeroy(), remplacant);
    JournalDAtelier annule = corrige.annule(remplacant.id(), annulationParLeroy());

    assertThat(annule.evenement(remplacant.id()).orElseThrow().remplace()).contains(fin.id());
    assertThat(annule.evenement(fin.id()).orElseThrow().remplace()).isEmpty();
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

    assertThat(termine.activites(Optional.empty()))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_7H30);
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_12H);
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

    assertThat(regularise.activites(Optional.empty()))
      .extracting(Activite::debut, Activite::categorie, Activite::fin)
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

    assertThat(journal.activites(Optional.empty()))
      .extracting(Activite::categorie, Activite::debut, Activite::fin)
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

    assertThat(journal.activites(Optional.empty()))
      .extracting(Activite::categorie, Activite::debut, Activite::fin)
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

    List<Activite> activites = journal.activites(Optional.empty());

    assertThat(activites)
      .extracting(Activite::debut, Activite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_8H)),
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H))
      );
    assertThat(duree(activites)).isEqualTo(Duration.ofHours(4));
  }

  @Test
  void shouldNeRienAjouterSurUnDoubleAppuiDecale() {
    EvenementDAtelier secondAppui = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H.plusSeconds(3));
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), secondAppui, finDe(secondAppui).a(LE_10_MAI_2026_A_12H))
    );

    assertThat(duree(journal.activites(Optional.empty()))).isEqualTo(Duration.ofHours(4));
  }

  @Test
  void shouldArreterUneActiviteRelancee() {
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), relance, finDe(relance).a(LE_10_MAI_2026_A_17H))
    );

    assertThat(journal.activites(Optional.of(LE_11_MAI_2026_A_9H15)))
      .extracting(Activite::debut, Activite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_13H)),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  /**
   * Un debut regularise au milieu d'une activite deja terminee la remplace a son heure : la fin pointee plus tard vise
   * alors une activite deja remplacee. L'acte du gestionnaire est admis, et la sequence est en conflit.
   */
  @Test
  void shouldConserverEnConflitUnDebutRegulariseAuMilieuDUneActiviteDejaTerminee() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_17H)));
    EvenementDAtelier debutRegularise = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_12H);

    JournalDAtelier regularise = journal.enregistre(debutRegularise);

    assertThat(regularise.activites(Optional.empty()))
      .extracting(Activite::ouvrant, Activite::aResoudre)
      .containsExactly(tuple(debut, true), tuple(debutRegularise, true));
  }

  @Test
  void shouldAnnulerLaRelanceDUneActivite() {
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), relance));

    JournalDAtelier corrige = journal.annule(relance.id(), annulationParLeroy());

    assertThat(corrige.activites(Optional.empty()))
      .extracting(Activite::debut, Activite::fin)
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

    assertThat(corrige.activites(Optional.empty()))
      .extracting(Activite::debut, Activite::fin)
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

    assertThat(corrige.activites(Optional.empty()))
      .extracting(Activite::categorie, Activite::debut, Activite::fin)
      .containsExactly(
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_12H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  @Test
  void shouldNotReadEvenementInconnu() {
    assertThat(JournalDAtelier.vide().evenement(EvenementDAtelierId.newId())).isEmpty();
  }

  /**
   * L'interpretation du journal obtenu en enregistrant les faits dans chaque ordre de reception possible : une
   * ouverture precede toujours les gestes qui la visent, un geste recu avant elle etant refuse.
   */
  private static Set<Interpretation> interpretationsSelonLOrdreDeReception(List<EvenementDAtelier> faits) {
    return permutations(faits)
      .stream()
      .filter(JournalDAtelierTest::ouvreAvantDeViser)
      .map(ordre -> {
        JournalDAtelier journal = JournalDAtelier.vide();
        for (EvenementDAtelier fait : ordre) {
          journal = journal.enregistre(fait);
        }
        return new Interpretation(journal.activites(Optional.empty()), journal.conflits(Optional.empty()));
      })
      .collect(Collectors.toSet());
  }

  private static boolean ouvreAvantDeViser(List<EvenementDAtelier> ordre) {
    return IntStream.range(0, ordre.size()).allMatch(rang ->
      ordre
        .get(rang)
        .activiteVisee()
        .map(visee ->
          ordre
            .subList(0, rang)
            .stream()
            .anyMatch(fait -> fait.activite().filter(visee::equals).isPresent())
        )
        .orElse(true)
    );
  }

  private static List<List<EvenementDAtelier>> permutations(List<EvenementDAtelier> faits) {
    if (faits.isEmpty()) {
      return List.of(List.of());
    }

    List<List<EvenementDAtelier>> permutations = new ArrayList<>();
    for (EvenementDAtelier premier : faits) {
      List<EvenementDAtelier> reste = new ArrayList<>(faits);
      reste.remove(premier);
      permutations(reste).forEach(suite -> {
        List<EvenementDAtelier> ordre = new ArrayList<>(List.of(premier));
        ordre.addAll(suite);
        permutations.add(ordre);
      });
    }
    return permutations;
  }

  private record Interpretation(List<Activite> activites, List<SequenceEnConflit> conflits) {}

  private static Duration duree(List<Activite> activites) {
    return activites
      .stream()
      .map(activite -> Duration.between(activite.debut(), activite.fin().orElseThrow()))
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
      .remplace(Optional.empty())
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
      .remplace(Optional.empty())
      .horodatage(Horodatage.saisiA(date));
  }
}

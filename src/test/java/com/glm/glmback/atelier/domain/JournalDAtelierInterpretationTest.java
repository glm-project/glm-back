package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * L'interpretation d'une cle : l'echeance, a treize heures de son debut, au-dela de laquelle une activite que rien n'a
 * terminee n'est plus vivante pour les gestes pointes ; et les sequences en conflit, ou des pointages contradictoires
 * sont conserves sans qu'aucune de leurs lectures ne soit choisie.
 */
@UnitTest
class JournalDAtelierInterpretationTest {

  private static final Instant LE_10_MAI_2026_A_10H = Instant.parse("2026-05-10T10:00:00Z");
  private static final Instant LE_10_MAI_2026_A_11H = Instant.parse("2026-05-10T11:00:00Z");
  private static final Instant LE_10_MAI_2026_A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant LE_10_MAI_2026_A_22H = Instant.parse("2026-05-10T22:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H = Instant.parse("2026-05-10T23:00:00Z");

  private static List<Activite> activites(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    return new JournalDAtelier(faits).activites(cloture);
  }

  private static List<SequenceEnConflit> conflits(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    return new JournalDAtelier(faits).conflits(cloture);
  }

  /**
   * FIN 23 h apres fin automatique 21 h : la fin pointee apres l'echeance est conservee sans effet ; l'activite garde
   * sa borne automatique, et aucun conflit n'est leve.
   */
  @Test
  void shouldConserverSansEffetUneFinPointeeApresLEcheance() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_23H)), Optional.empty());

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.ouvrant()).isEqualTo(travail);
        assertThat(activite.fin()).isEmpty();
        assertThat(activite.a(LE_11_MAI_2026_A_9H15).fin()).contains(LE_10_MAI_2026_A_21H);
      });
  }

  /**
   * Transition NC ciblant le travail de 08 h, pointee a 23 h : le travail garde sa borne automatique de 21 h, la non
   * conformite s'ouvre a 23 h, et rien ne couvre 21 h - 23 h. Une cible expiree ne contredit aucun fait.
   */
  @Test
  void shouldOuvrirALHeureDuGesteUneTransitionPointeeApresLEcheanceDeSaCible() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_23H);

    List<Activite> activites = activites(List.of(travail, nonConformite), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::debut, Activite::fin)
      .containsExactly(tuple(travail, LE_10_MAI_2026_A_8H, Optional.empty()), tuple(nonConformite, LE_10_MAI_2026_A_23H, Optional.empty()));
    assertThat(activites.getLast().echeance()).isEqualTo(new Echeance(Instant.parse("2026-05-11T12:00:00Z")));
  }

  /**
   * Regularisation d'une fin a 23 h apres fin automatique a 21 h : l'acte du gestionnaire etablit une fin reelle
   * au-dela de l'echeance, et l'activite compte ses 15 h.
   */
  @Test
  void shouldTerminerAuDelaDeLEcheanceParUneFinRegularisee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail, finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_23H)), Optional.empty());

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_23H);
        assertThat(activite.a(LE_11_MAI_2026_A_9H15).finAutomatique()).isFalse();
      });
  }

  /**
   * Le pouvoir de depasser l'echeance vaut aussi pour une transition regularisee : le travail se termine a 23 h, et la
   * non conformite s'ouvre a la meme heure.
   */
  @Test
  void shouldRemplacerAuDelaDeLEcheanceParUneTransitionRegularisee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteRegulariseParLeroyDe(travail).a(LE_10_MAI_2026_A_23H);

    List<Activite> activites = activites(List.of(travail, nonConformite), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(travail, Optional.of(LE_10_MAI_2026_A_23H)), tuple(nonConformite, Optional.empty()));
  }

  /**
   * Seule la regularisation porte l'activite au-dela de son echeance : la fin pointee a 22 h reste sans effet, meme
   * quand le gestionnaire a regularise la fin a 23 h.
   */
  @Test
  void shouldLaisserSansEffetUneFinPointeeApresLEcheanceMemeAvantUneFinRegularisee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier finPointee = finDe(travail).a(Instant.parse("2026-05-10T22:00:00Z"));
    EvenementDAtelier finRegularisee = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_23H);

    List<Activite> activites = activites(List.of(travail, finPointee, finRegularisee), Optional.empty());

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_23H));
  }

  /**
   * FIN exactement a l'echeance : le geste reel l'emporte sur la fin automatique, sans anomalie.
   */
  @Test
  void shouldTerminerParUneFinPointeeExactementALEcheance() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_21H)), Optional.empty());

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_21H);
        assertThat(activite.a(LE_11_MAI_2026_A_9H15).finAutomatique()).isFalse();
      });
  }

  /**
   * Transition exactement a l'echeance : elle remplace sa cible a son heure, qui se termine sans anomalie.
   */
  @Test
  void shouldRemplacerParUneTransitionPointeeExactementALEcheance() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_21H);

    List<Activite> activites = activites(List.of(travail, nonConformite), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(travail, Optional.of(LE_10_MAI_2026_A_21H)), tuple(nonConformite, Optional.empty()));
  }

  /**
   * Relance avant l'echeance : l'activite precedente se termine a la relance.
   */
  @Test
  void shouldTerminerALaRelanceUneActiviteRelanceeAvantSonEcheance() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);

    List<Activite> activites = activites(List.of(premiere, relance), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(premiere, Optional.of(LE_10_MAI_2026_A_12H)), tuple(relance, Optional.empty()));
  }

  /**
   * Relance apres l'echeance : l'activite precedente garde sa borne automatique de 21 h, la nouvelle s'ouvre a 23 h ;
   * la relance ne la prolonge pas a travers le trou.
   */
  @Test
  void shouldLaisserSaBorneAutomatiqueAUneActiviteRelanceeApresSonEcheance() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_23H);

    List<Activite> activites = activites(List.of(premiere, relance), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(premiere, Optional.empty()), tuple(relance, Optional.empty()));
  }

  /**
   * Une cloture du suivi posterieure a l'echeance ne prolonge pas l'activite : elle garde sa borne automatique.
   */
  @Test
  void shouldNePasProlongerParLaClotureUneActiviteDejaEchue() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail), Optional.of(LE_10_MAI_2026_A_23H));

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.empty());
  }

  /**
   * Une cloture avant l'echeance termine l'activite restee en cours, a son heure.
   */
  @Test
  void shouldTerminerALaClotureUneActiviteEncoreVivante() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail), Optional.of(LE_10_MAI_2026_A_17H));

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_17H));
  }

  /**
   * La table du plan : travail A a 08 h, transition A -> NC a 12 h, fin de A a 17 h. La fin vise une activite deja
   * remplacee a son heure : la sequence est en conflit. A et la non conformite sont a resoudre, sans fin ; la fin de A
   * n'est pas reaffectee a la non conformite, ni la transition ignoree.
   */
  @Test
  void shouldMettreEnConflitUneFinQuiViseUneActiviteDejaRemplaceeParUneTransition() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_17H);
    List<EvenementDAtelier> faits = List.of(travail, nonConformite, finDuTravail);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::fin, Activite::aResoudre)
      .containsExactly(tuple(travail, Optional.empty(), true), tuple(nonConformite, Optional.empty(), true));
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> {
        assertThat(conflit.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
        assertThat(conflit.activites()).containsExactly(activiteDe(travail), activiteDe(nonConformite));
        assertThat(conflit.pointages()).containsExactly(travail.id(), nonConformite.id(), finDuTravail.id());
      });
  }

  /**
   * Geste visant A apres remplacement par B : A a 08 h, relance B a 10 h, fin de A a 11 h. La fin ne termine jamais B ;
   * A et B sont a resoudre, et B n'est plus en cours.
   */
  @Test
  void shouldMettreEnConflitUneFinQuiViseUneActiviteRelancee() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier finDeA = finDe(a).a(LE_10_MAI_2026_A_11H);
    List<EvenementDAtelier> faits = List.of(a, b, finDeA);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::fin, Activite::aResoudre)
      .containsExactly(tuple(a, Optional.empty(), true), tuple(b, Optional.empty(), true));
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> {
        assertThat(conflit.activites()).containsExactly(activiteDe(a), activiteDe(b));
        assertThat(conflit.pointages()).containsExactly(a.id(), b.id(), finDeA.id());
      });
  }

  /**
   * Une seconde fin sur une activite deja terminee par une fin reelle, meme deux secondes apres la premiere : le double
   * appui n'est plus absorbe, il met la sequence en conflit.
   */
  @Test
  void shouldMettreEnConflitUneSecondeFinDeLaMemeActivite() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier premiere = finDe(travail).a(LE_10_MAI_2026_A_10H);
    EvenementDAtelier seconde = finDe(travail).a(LE_10_MAI_2026_A_10H.plusSeconds(2));
    List<EvenementDAtelier> faits = List.of(travail, premiere, seconde);

    assertThat(activites(faits, Optional.empty()))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.aResoudre()).isTrue();
        assertThat(activite.fin()).isEmpty();
      });
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(travail.id(), premiere.id(), seconde.id()));
  }

  /**
   * Une transition apres la fin reelle de sa cible reste une transition : elle ne devient pas une ouverture, elle met
   * la sequence en conflit avec l'activite qu'elle ouvre.
   */
  @Test
  void shouldMettreEnConflitUneTransitionQuiViseUneActiviteDejaTerminee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_10H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_11H);
    List<EvenementDAtelier> faits = List.of(travail, fin, nonConformite);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::aResoudre)
      .containsExactly(tuple(travail, true), tuple(nonConformite, true));
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(travail.id(), fin.id(), nonConformite.id()));
  }

  /**
   * Une transition vers la meme categorie n'est pas une relance deguisee : conservee, elle met la sequence en conflit,
   * sa cible en cours comme sa cible echue.
   */
  @Test
  void shouldMettreEnConflitUneTransitionDeMemeCategorie() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    for (Instant heure : List.of(LE_10_MAI_2026_A_9H, LE_10_MAI_2026_A_23H)) {
      EvenementDAtelier travailSurTravail = passageEnTravailDe(travail).a(heure);
      List<EvenementDAtelier> faits = List.of(travail, travailSurTravail);

      assertThat(activites(faits, Optional.empty()))
        .describedAs("transition a %s", heure)
        .extracting(Activite::ouvrant, Activite::aResoudre)
        .containsExactly(tuple(travail, true), tuple(travailSurTravail, true));
      assertThat(conflits(faits, Optional.empty())).describedAs("transition a %s", heure).hasSize(1);
    }
  }

  /**
   * A 08 h echue a 21 h, relance B a 22 h, NC visant A a 23 h : la transition ne peut ouvrir sa non conformite sans
   * terminer B, qu'elle ne vise pas. La sequence est en conflit.
   */
  @Test
  void shouldMettreEnConflitUneTransitionVersUneCibleEchueQuandUneAutreActiviteEstEnCours() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_22H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(a).a(LE_10_MAI_2026_A_23H);
    List<EvenementDAtelier> faits = List.of(a, b, nonConformite);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::aResoudre)
      .containsExactly(tuple(a, true), tuple(b, true), tuple(nonConformite, true));
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(a.id(), b.id(), nonConformite.id()));
  }

  /**
   * Transition pointee apres l'echeance de sa cible, que le gestionnaire a prolongee plus tard par une fin
   * regularisee : la cible vit encore a l'heure de la transition, qu'un pointage ne peut plus terminer. La transition
   * n'ouvre pas sa non conformite par-dessus une activite en cours : la sequence est en conflit.
   */
  @Test
  void shouldMettreEnConflitUneTransitionPointeeApresLEcheanceDUneCibleProlongeeParLeGestionnaire() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_22H);
    EvenementDAtelier finRegularisee = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_23H);
    List<EvenementDAtelier> faits = List.of(travail, nonConformite, finRegularisee);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::aResoudre)
      .containsExactly(tuple(travail, true), tuple(nonConformite, true));
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(travail.id(), nonConformite.id(), finRegularisee.id()));
  }

  /**
   * A 08 h relancee par B a 22 h, puis une fin de A regularisee a 23 h : prolongee par le gestionnaire, A vivait encore
   * a 22 h, et la relance l'a remplacee avant la fin qui la vise. La sequence est en conflit.
   */
  @Test
  void shouldMettreEnConflitUneFinRegulariseeDUneActiviteDejaRelancee() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_22H);
    EvenementDAtelier finRegularisee = finRegulariseeParLeroyDe(a).a(LE_10_MAI_2026_A_23H);
    List<EvenementDAtelier> faits = List.of(a, b, finRegularisee);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::aResoudre)
      .containsExactly(tuple(a, true), tuple(b, true));
    assertThat(conflits(faits, Optional.empty())).singleElement();
  }

  /**
   * Une fin datee avant le debut de l'activite qu'elle vise, apres une correction de ce debut par exemple, contredit
   * le journal : la sequence est en conflit, et la fin ne termine rien.
   */
  @Test
  void shouldMettreEnConflitUneFinAnterieureAuDebutDeSaCible() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finAnterieure = finDe(travail).a(LE_10_MAI_2026_A_9H);
    List<EvenementDAtelier> faits = List.of(finAnterieure, travail);

    assertThat(activites(faits, Optional.empty()))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.aResoudre()).isTrue();
        assertThat(activite.fin()).isEmpty();
      });
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(finAnterieure.id(), travail.id()));
  }

  /**
   * Hors de la zone contradictoire, les activites de la cle gardent leur interpretation : une fin anterieure au conflit
   * reste une fin, et l'ouverture pointee apres lui est en cours, actionnable.
   */
  @Test
  void shouldLaisserInterpretablesLesActivitesHorsDeLaZoneContradictoire() {
    EvenementDAtelier avant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_7H);
    EvenementDAtelier finAvant = finDe(avant).a(LE_10_MAI_2026_A_7H30);
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier finDeA = finDe(a).a(LE_10_MAI_2026_A_11H);
    EvenementDAtelier apres = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    List<EvenementDAtelier> faits = List.of(avant, finAvant, a, b, finDeA, apres);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::fin, Activite::aResoudre)
      .containsExactly(
        tuple(avant, Optional.of(LE_10_MAI_2026_A_7H30), false),
        tuple(a, Optional.empty(), true),
        tuple(b, Optional.empty(), true),
        tuple(apres, Optional.empty(), false)
      );
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.activites()).containsExactly(activiteDe(a), activiteDe(b)));
  }

  /**
   * Un geste qui vise une activite a resoudre rejoint sa sequence : la fin de B, pointee apres la fin contradictoire
   * de A, ne rend pas B interpretable.
   */
  @Test
  void shouldRattacherALaSequenceUnGesteQuiViseUneActiviteAResoudre() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier finDeA = finDe(a).a(LE_10_MAI_2026_A_11H);
    EvenementDAtelier finDeB = finDe(b).a(LE_10_MAI_2026_A_12H);
    List<EvenementDAtelier> faits = List.of(a, b, finDeA, finDeB);

    assertThat(activites(faits, Optional.empty())).extracting(Activite::aResoudre).containsExactly(true, true);
    assertThat(conflits(faits, Optional.empty()))
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).containsExactly(a.id(), b.id(), finDeA.id(), finDeB.id()));
  }

  /**
   * Deux contradictions separees par des activites interpretables forment deux sequences distinctes sur la meme cle.
   */
  @Test
  void shouldSeparerDeuxSequencesEnConflitDisjointes() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    EvenementDAtelier finDeA = finDe(a).a(LE_10_MAI_2026_A_10H);
    EvenementDAtelier c = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDeC = finDe(c).a(LE_10_MAI_2026_A_13H);
    EvenementDAtelier d = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_16H);
    EvenementDAtelier e = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H);
    EvenementDAtelier finDeD = finDe(d).a(LE_10_MAI_2026_A_20H);
    List<EvenementDAtelier> faits = List.of(a, b, finDeA, c, finDeC, d, e, finDeD);

    assertThat(activites(faits, Optional.empty()))
      .extracting(Activite::ouvrant, Activite::aResoudre)
      .containsExactly(tuple(a, true), tuple(b, true), tuple(c, false), tuple(d, true), tuple(e, true));
    assertThat(conflits(faits, Optional.empty()))
      .extracting(SequenceEnConflit::activites)
      .containsExactly(List.of(activiteDe(a), activiteDe(b)), List.of(activiteDe(d), activiteDe(e)));
  }

  /**
   * La cloture ne tranche pas un conflit : l'activite a resoudre reste sans fin.
   */
  @Test
  void shouldNePasTerminerParLaClotureUneActiviteAResoudre() {
    EvenementDAtelier a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier b = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    List<EvenementDAtelier> faits = List.of(a, b, finDe(a).a(LE_10_MAI_2026_A_11H));

    assertThat(activites(faits, Optional.of(LE_10_MAI_2026_A_17H)))
      .extracting(Activite::fin, Activite::aResoudre)
      .containsExactly(tuple(Optional.empty(), true), tuple(Optional.empty(), true));
  }

  /**
   * Une cible seulement echue ne contredit rien : ni la fin pointee apres l'echeance, ni la transition qui ouvre sa
   * non conformite apres elle, ne laissent de sequence en conflit.
   */
  @Test
  void shouldNeLaisserAucunConflitQuandLaCibleEstSeulementEchue() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_22H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_23H);

    assertThat(conflits(List.of(travail, fin), Optional.empty())).isEmpty();
    assertThat(conflits(List.of(travail, nonConformite), Optional.empty())).isEmpty();
    assertThat(activites(List.of(travail, fin, nonConformite), Optional.empty()))
      .extracting(Activite::aResoudre)
      .containsExactly(false, false);
  }

  /**
   * Un geste exactement a l'echeance de sa cible la termine : aucun conflit.
   */
  @Test
  void shouldNeLaisserAucunConflitPourUnGesteExactementALEcheance() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    assertThat(conflits(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_21H)), Optional.empty())).isEmpty();
    assertThat(conflits(List.of(travail, passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_21H)), Optional.empty())).isEmpty();
  }

  private static ActiviteId activiteDe(EvenementDAtelier ouvrant) {
    return ouvrant.activite().orElseThrow();
  }
}

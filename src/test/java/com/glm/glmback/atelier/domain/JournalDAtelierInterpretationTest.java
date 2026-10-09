package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La lecture d'une cle, seule interpretation du journal : l'echeance, a treize heures de son debut, a partir de laquelle
 * une activite que rien n'a terminee est lue comme terminee automatiquement ; la fin pointee, qui ferme l'activite en
 * cours de la cle sans la designer ; et la fin regularisee, qui ferme celle qu'elle cible.
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

  /**
   * Une fin pointee ferme l'activite en cours de sa cle : le fait ne designe rien, la cle le dit.
   */
  @Test
  void shouldTerminerLActiviteEnCoursDeLaCleParUneFinPointee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_12H)), Optional.empty());

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_12H);
        assertThat(activite.a(LE_11_MAI_2026_A_9H15).finAutomatique()).isFalse();
      });
  }

  /**
   * Une fin sur une cle ou rien n'est en cours, ou apres une autre fin, n'a aucun effet : elle ne rouvre rien et ne
   * ferme aucune autre activite.
   */
  @Test
  void shouldIgnorerUneFinQuandRienNEstEnCoursSurLaCle() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier premiereFin = finDe(travail).a(LE_10_MAI_2026_A_10H);

    assertThat(activites(List.of(finDe(travail).a(LE_10_MAI_2026_A_7H)), Optional.empty())).isEmpty();
    assertThat(activites(List.of(travail, premiereFin, finDe(travail).a(LE_10_MAI_2026_A_12H)), Optional.empty()))
      .singleElement()
      .extracting(Activite::fin)
      .isEqualTo(Optional.of(LE_10_MAI_2026_A_10H));
  }

  /**
   * La cle est l'operateur et le poste : la fin de Martin ne ferme pas l'activite de Dupont.
   */
  @Test
  void shouldLaisserEnCoursLActiviteDUneAutreCle() {
    EvenementDAtelier travailDeDupont = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier travailDeMartin = debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(
      List.of(travailDeDupont, travailDeMartin, finDe(travailDeMartin).a(LE_10_MAI_2026_A_12H)),
      Optional.empty()
    );

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactlyInAnyOrder(tuple(travailDeDupont, Optional.empty()), tuple(travailDeMartin, Optional.of(LE_10_MAI_2026_A_12H)));
  }

  /**
   * Une fin et l'ouverture qui la suit a la meme heure — le passage en non conformite d'un seul appui — se lisent la fin
   * d'abord, quel que soit l'ordre dans lequel le journal les a recus : le travail se termine, la non conformite
   * s'ouvre.
   */
  @Test
  void shouldLireLaFinAvantLOuvertureDeLaMemeHeure() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);

    List<Activite> activites = activites(List.of(nonConformite, travail, fin), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(travail, Optional.of(LE_10_MAI_2026_A_12H)), tuple(nonConformite, Optional.empty()));
  }

  /**
   * FIN 23 h apres fin automatique 21 h : la fin pointee apres l'echeance n'a aucun effet ; l'activite garde sa borne
   * automatique.
   */
  @Test
  void shouldIgnorerUneFinPointeeApresLEcheance() {
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
   * L'echeance est atteinte a debut plus 13 h : une fin pile a 21 h ne l'emporte pas sur la fin automatique, comme la
   * regle de reception qui l'ignore (APRES_ECHEANCE).
   */
  @Test
  void shouldLireCommeFinAutomatiqueUneFinPointeeExactementALEcheance() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_21H)), Optional.empty());

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).isEmpty();
        assertThat(activite.a(LE_11_MAI_2026_A_9H15).fin()).contains(LE_10_MAI_2026_A_21H);
        assertThat(activite.a(LE_11_MAI_2026_A_9H15).finAutomatique()).isTrue();
      });
  }

  /**
   * Un debut pile a l'echeance de l'activite en cours est accepte par la regle : la lecture donne a l'activite
   * precedente une fin automatique a 21 h, jamais une fin reelle, et ouvre la suivante a 21 h.
   */
  @Test
  void shouldTerminerAutomatiquementUneActiviteDontLEcheanceEstCelleDUnDebut() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier suivante = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_21H);

    List<Activite> activites = activites(List.of(premiere, suivante), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(premiere, Optional.empty()), tuple(suivante, Optional.empty()));
    assertThat(activites.getFirst().a(LE_11_MAI_2026_A_9H15)).satisfies(lue -> {
      assertThat(lue.fin()).contains(LE_10_MAI_2026_A_21H);
      assertThat(lue.finAutomatique()).isTrue();
    });
  }

  /**
   * Un debut apres l'echeance : l'activite precedente garde sa borne automatique de 21 h, la nouvelle s'ouvre a 23 h ;
   * rien ne prolonge la premiere a travers le trou.
   */
  @Test
  void shouldLaisserSaBorneAutomatiqueAUneActiviteSuivieApresSonEcheance() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier suivante = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_23H);

    List<Activite> activites = activites(List.of(premiere, suivante), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(premiere, Optional.empty()), tuple(suivante, Optional.empty()));
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
   * Seule la regularisation porte l'activite au-dela de son echeance : la fin pointee a 22 h reste sans effet, meme
   * quand le gestionnaire a regularise la fin a 23 h.
   */
  @Test
  void shouldLaisserSansEffetUneFinPointeeApresLEcheanceMemeAvantUneFinRegularisee() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier finPointee = finDe(travail).a(LE_10_MAI_2026_A_22H);
    EvenementDAtelier finRegularisee = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_23H);

    List<Activite> activites = activites(List.of(travail, finPointee, finRegularisee), Optional.empty());

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_23H));
  }

  /**
   * La fin regularisee ferme l'activite qu'elle cible, pas celle qui est en cours : l'activite echue prend sa fin a
   * 22 h, et l'ouverture qui la suit a 23 h reste en cours.
   */
  @Test
  void shouldTerminerLaCibleDUneFinRegulariseeEtLaisserEnCoursLActiviteSuivante() {
    EvenementDAtelier echue = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier suivante = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_23H);
    EvenementDAtelier finRegularisee = finRegulariseeParLeroyDe(echue).a(LE_10_MAI_2026_A_22H);

    List<Activite> activites = activites(List.of(echue, suivante, finRegularisee), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(echue, Optional.of(LE_10_MAI_2026_A_22H)), tuple(suivante, Optional.empty()));
  }

  /**
   * Une fin regularisee avant l'echeance ferme l'activite alors en cours : la cle est libre pour l'ouverture suivante.
   */
  @Test
  void shouldTerminerParUneFinRegulariseeAvantLEcheanceLActiviteEnCours() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier finRegularisee = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier suivante = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);

    List<Activite> activites = activites(List.of(travail, finRegularisee, suivante), Optional.empty());

    assertThat(activites)
      .extracting(Activite::ouvrant, Activite::fin)
      .containsExactly(tuple(travail, Optional.of(LE_10_MAI_2026_A_12H)), tuple(suivante, Optional.empty()));
  }

  /**
   * Une fin regularisee dont la cible n'est pas une activite de la cle n'a aucun effet.
   */
  @Test
  void shouldIgnorerUneFinRegulariseeDontLaCibleEstInconnueDeLaCle() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier inconnue = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_7H);

    List<Activite> activites = activites(List.of(travail, finRegulariseeParLeroyDe(inconnue).a(LE_10_MAI_2026_A_12H)), Optional.empty());

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.empty());
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
   * Une cloture a l'echeance exacte la trouve atteinte : l'activite garde sa borne automatique.
   */
  @Test
  void shouldNePasTerminerParLaClotureUneActiviteDontLEcheanceEstAtteinte() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = activites(List.of(travail), Optional.of(LE_10_MAI_2026_A_21H));

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
   * La situation 1 de la regle : travail, non conformite, travail, chacun fermant le precedent a la meme heure.
   */
  @Test
  void shouldLireUneJourneeDeTravailEtDeNonConformite() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_7H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_10H);
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier finDeLaNonConformite = finDe(nonConformite).a(LE_10_MAI_2026_A_11H);
    EvenementDAtelier reprise = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_11H);
    EvenementDAtelier arret = finDe(reprise).a(LE_10_MAI_2026_A_12H);

    List<Activite> activites = activites(
      List.of(travail, finDuTravail, nonConformite, finDeLaNonConformite, reprise, arret),
      Optional.empty()
    );

    assertThat(activites)
      .extracting(Activite::categorie, Activite::debut, Activite::fin)
      .containsExactly(
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_10H)),
        tuple(CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_10H, Optional.of(LE_10_MAI_2026_A_11H)),
        tuple(CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_11H, Optional.of(LE_10_MAI_2026_A_12H))
      );
  }
}

package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * L'echeance dans l'interpretation d'une cle : a treize heures de son debut, une activite que rien n'a terminee n'est
 * plus vivante pour les gestes pointes, sans qu'aucun fait ne soit ecrit.
 */
@UnitTest
class SequenceDActivitesTest {

  private static final Instant LE_10_MAI_2026_A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H = Instant.parse("2026-05-10T23:00:00Z");

  /**
   * FIN 23 h apres fin automatique 21 h : la fin pointee apres l'echeance est conservee sans effet ; l'activite garde
   * sa borne automatique, et aucun conflit n'est leve.
   */
  @Test
  void shouldConserverSansEffetUneFinPointeeApresLEcheance() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = SequenceDActivites.activites(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_23H)), Optional.empty());

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

    List<Activite> activites = SequenceDActivites.activites(List.of(travail, nonConformite), Optional.empty());

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

    List<Activite> activites = SequenceDActivites.activites(
      List.of(travail, finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_23H)),
      Optional.empty()
    );

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

    List<Activite> activites = SequenceDActivites.activites(List.of(travail, nonConformite), Optional.empty());

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

    List<Activite> activites = SequenceDActivites.activites(List.of(travail, finPointee, finRegularisee), Optional.empty());

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_23H));
  }

  /**
   * FIN exactement a l'echeance : le geste reel l'emporte sur la fin automatique, sans anomalie.
   */
  @Test
  void shouldTerminerParUneFinPointeeExactementALEcheance() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = SequenceDActivites.activites(List.of(travail, finDe(travail).a(LE_10_MAI_2026_A_21H)), Optional.empty());

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

    List<Activite> activites = SequenceDActivites.activites(List.of(travail, nonConformite), Optional.empty());

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

    List<Activite> activites = SequenceDActivites.activites(List.of(premiere, relance), Optional.empty());

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

    List<Activite> activites = SequenceDActivites.activites(List.of(premiere, relance), Optional.empty());

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

    List<Activite> activites = SequenceDActivites.activites(List.of(travail), Optional.of(LE_10_MAI_2026_A_23H));

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.empty());
  }

  /**
   * Une cloture avant l'echeance termine l'activite restee en cours, a son heure.
   */
  @Test
  void shouldTerminerALaClotureUneActiviteEncoreVivante() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    List<Activite> activites = SequenceDActivites.activites(List.of(travail), Optional.of(LE_10_MAI_2026_A_17H));

    assertThat(activites).singleElement().extracting(Activite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_17H));
  }

  /**
   * Une transition vers la meme categorie reste une contradiction, meme quand sa cible a expire.
   */
  @Test
  void shouldRefuserUneTransitionDeMemeCategorieVersUneCibleExpiree() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    List<EvenementDAtelier> faits = List.of(travail, passageEnTravailDe(travail).a(LE_10_MAI_2026_A_23H));

    assertThatThrownBy(() -> SequenceDActivites.activites(faits, Optional.empty())).isExactlyInstanceOf(
      TransitionDAtelierInterditeException.class
    );
  }

  /**
   * Une transition dont la cible a expire alors qu'une autre activite est en cours sur la cle ne peut pas ouvrir la
   * sienne sans terminer celle-ci : elle contredit le journal.
   */
  @Test
  void shouldRefuserUneTransitionVersUneCibleExpireeQuandUneAutreActiviteEstEnCours() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    List<EvenementDAtelier> faits = List.of(
      travail,
      debutSurFraiseuse1ParDupontA(Instant.parse("2026-05-10T22:00:00Z")),
      passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_23H)
    );

    assertThatThrownBy(() -> SequenceDActivites.activites(faits, Optional.empty())).isExactlyInstanceOf(
      TransitionDAtelierInterditeException.class
    );
  }
}

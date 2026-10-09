package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
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
    assertThat(journal.evenements()).isEmpty();
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
  void shouldDepartagerLesSaisiesIdentiquesParId() {
    EvenementDAtelier premier = ouvertureNumero(1, LE_10_MAI_2026_A_8H);
    EvenementDAtelier second = ouvertureNumero(2, LE_10_MAI_2026_A_8H);

    JournalDAtelier journal = new JournalDAtelier(List.of(second, premier));

    assertThat(journal.evenements()).containsExactly(premier, second);
  }

  /**
   * Relu tel quel, un journal ne se refuse jamais : une fin qui ne ferme rien n'a aucun effet, jamais une exception qui
   * bloquerait la relecture.
   */
  @Test
  void shouldConserverSansEffetUneFinSansActiviteEnCours() {
    EvenementDAtelier jamaisEnregistre = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier finSansDebut = finDe(jamaisEnregistre).a(LE_10_MAI_2026_A_12H);

    JournalDAtelier journal = new JournalDAtelier(List.of(finSansDebut));

    assertThat(journal.evenements()).containsExactly(finSansDebut);
    assertThat(journal.activites(Optional.empty())).isEmpty();
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
   * Travail, non conformite puis travail : chaque fin ferme l'activite en cours et l'ouverture qui la suit a la meme
   * heure en ouvre une distincte, sans trou ni recouvrement.
   */
  @Test
  void shouldEnchainerTravailNonConformitePuisTravailEnActivitesDistinctes() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    EvenementDAtelier reprise = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);

    JournalDAtelier journal = new JournalDAtelier(
      List.of(
        travail,
        finDe(travail).a(LE_10_MAI_2026_A_9H),
        nonConformite,
        finDe(nonConformite).a(LE_10_MAI_2026_A_12H),
        reprise,
        finDe(reprise).a(LE_10_MAI_2026_A_13H)
      )
    );

    assertThat(journal.activites(Optional.empty()))
      .extracting(Activite::ouvrant, Activite::categorie, Activite::debut, Activite::fin)
      .containsExactly(
        tuple(travail, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_9H)),
        tuple(nonConformite, CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(reprise, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_12H, Optional.of(LE_10_MAI_2026_A_13H))
      );
  }

  /**
   * Le journal se relit en entier : une fin enregistree apres coup, a une heure plus ancienne que celle qu'il porte
   * deja, ferme l'activite a son heure, et la fin posterieure ne ferme plus rien.
   */
  @Test
  void shouldRelireToutLeJournalSurUneInsertionRetroactive() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finDe(debut).a(LE_10_MAI_2026_A_17H)));

    JournalDAtelier relu = journal.enregistre(finDe(debut).a(LE_10_MAI_2026_A_12H));

    assertThat(journal.activites(Optional.empty())).extracting(Activite::fin).containsExactly(Optional.of(LE_10_MAI_2026_A_17H));
    assertThat(relu.activites(Optional.empty())).extracting(Activite::fin).containsExactly(Optional.of(LE_10_MAI_2026_A_12H));
  }

  /**
   * Une ouverture de Dupont sur la fraiseuse 1 dont l'identifiant est fixe : de quoi prouver qu'il ne departage que
   * des gestes simultanes de meme nature.
   */
  private static EvenementDAtelier ouvertureNumero(long numero, Instant date) {
    EvenementDAtelierId id = new EvenementDAtelierId(new UUID(0, numero));

    return EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
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
}

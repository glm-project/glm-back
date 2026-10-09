package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La regle de reception, telle que le suivi la juge : ANTERIEUR d'abord, puis l'echeance, puis le tableau de la cle. Un
 * pointage n'est jamais refuse ici, il est accepte ou ignore pour l'une des quatre raisons.
 */
@UnitTest
class SuiviDAtelierReceptionTest {

  private static final Instant A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant A_22H = Instant.parse("2026-05-10T22:00:00Z");
  private static final Instant A_23H = Instant.parse("2026-05-10T23:00:00Z");

  private final EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
  private final CleDActivite cle = cleDeFraiseuse1DeDupont();

  @Test
  void shouldAccepterUnDebutQuandRienNEstEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_8H)).isEqualTo(
      new VerdictDeReception.Accepte(Optional.empty())
    );
  }

  @Test
  void shouldAccepterUneNonConformiteQuandRienNEstEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.NON_CONFORMITE, LE_10_MAI_2026_A_8H)).isEqualTo(
      new VerdictDeReception.Accepte(Optional.empty())
    );
  }

  @Test
  void shouldAccepterUnDebutApresUneFinReelle() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_13H)).isEqualTo(
      new VerdictDeReception.Accepte(Optional.empty())
    );
  }

  @Test
  void shouldIgnorerUnDebutPendantUneActiviteEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, Optional.of(travail.id()))
    );
  }

  @Test
  void shouldIgnorerUneNonConformitePendantUneNonConformite() {
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(nonConformite);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.NON_CONFORMITE, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, Optional.of(nonConformite.id()))
    );
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, Optional.of(nonConformite.id()))
    );
  }

  @Test
  void shouldAccepterUneFinQuiTermineLActiviteEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_12H)).isEqualTo(
      new VerdictDeReception.Accepte(travail.activite())
    );
  }

  @Test
  void shouldIgnorerUneFinSurUneCleJamaisOuverte() {
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_12H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.empty())
    );
  }

  /**
   * Une heure egale passe : la seconde fin du double appui n'est pas anterieure, elle n'a simplement plus d'activite.
   */
  @Test
  void shouldIgnorerUneSecondeFinPointeeALaMemeHeure() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_12H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.of(fin.id()))
    );
  }

  @Test
  void shouldIgnorerUnPointagePlusAncienQueLeDernierAccepte() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);

    for (TypeDEvenementDAtelier type : TypeDEvenementDAtelier.values()) {
      assertThat(suivi.juge(cle, type, LE_10_MAI_2026_A_12H.minusNanos(1))).isEqualTo(
        new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(fin.id()))
      );
    }
  }

  /**
   * Une activite de duree nulle n'existe pas : une fin qui n'est pas posterieure au debut de l'activite qu'elle fermerait
   * est anterieure, et l'activite reste en cours. Une fin a t qui ferme une activite ouverte avant t, suivie d'une
   * ouverture a t, reste acceptee : les gestes composes du pupitre.
   */
  @Test
  void shouldIgnorerCommeAnterieureUneFinALHeureDuDebutDeLActiviteEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_8H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(travail.id()))
    );
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_8H.plusNanos(1))).isEqualTo(
      new VerdictDeReception.Accepte(travail.activite())
    );
  }

  @Test
  void shouldAccepterLesGestesComposesALaMemeHeure() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);
    SuiviDAtelier apresLaFin = suiviDAtelierEngage().enregistre(travail).enregistre(fin);

    assertThat(suiviDAtelierEngage().enregistre(travail).juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_12H)).isEqualTo(
      new VerdictDeReception.Accepte(travail.activite())
    );
    assertThat(apresLaFin.juge(cle, TypeDEvenementDAtelier.NON_CONFORMITE, LE_10_MAI_2026_A_12H)).isEqualTo(
      new VerdictDeReception.Accepte(Optional.empty())
    );
    assertThat(apresLaFin.enregistre(nonConformite).juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_13H)).isEqualTo(
      new VerdictDeReception.Accepte(nonConformite.activite())
    );
  }

  /**
   * A heure egale, le journal range la fin avant l'ouverture : le dernier accepte comparable est donc l'ouverture.
   */
  @Test
  void shouldComparerAuDernierAccepteDuJournalAHeureEgale() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier nonConformite = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin).enregistre(nonConformite);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(nonConformite.id()))
    );
  }

  @Test
  void shouldIgnorerUneFinPileALEcheance() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, A_21H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.APRES_ECHEANCE, Optional.of(travail.id()))
    );
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, A_21H.minusNanos(1))).isEqualTo(
      new VerdictDeReception.Accepte(travail.activite())
    );
  }

  @Test
  void shouldIgnorerUneSecondeFinSurUneActiviteEchueEtSansFin() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, A_22H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.APRES_ECHEANCE, Optional.of(travail.id()))
    );
  }

  /**
   * Une activite echue compte comme terminee : un demarrage a l'echeance, ou apres, ouvre une nouvelle activite.
   */
  @Test
  void shouldAccepterUnDebutQuandLActiviteEnCoursEstEchue() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, A_21H)).isEqualTo(new VerdictDeReception.Accepte(Optional.empty()));
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.NON_CONFORMITE, A_22H)).isEqualTo(new VerdictDeReception.Accepte(Optional.empty()));
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, A_21H.minusNanos(1))).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, Optional.of(travail.id()))
    );
  }

  /**
   * Seule la derniere activite de la cle compte : l'echeance d'une activite que la suivante a remplacee ne pese plus.
   */
  @Test
  void shouldNeJugerQueLaDerniereActiviteDeLaCle() {
    EvenementDAtelier relance = debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_7H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(relance);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_11_MAI_2026_A_8H)).isEqualTo(
      new VerdictDeReception.Accepte(relance.activite())
    );
  }

  @Test
  void shouldIgnorerUneFinSurUneActiviteDontLeGestionnaireARegulariseLaFin() {
    EvenementDAtelier regularisation = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(regularisation);

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, A_22H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.of(regularisation.id()))
    );
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_16H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(regularisation.id()))
    );
  }

  @Test
  void shouldJugerChaqueCleAPart() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);
    CleDActivite autrePoste = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2));
    CleDActivite autreOperateur = new CleDActivite(OPERATEUR_ID_MARTIN, Optional.of(POSTE_ID_FRAISEUSE_1));

    assertThat(suivi.juge(autrePoste, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_7H)).isEqualTo(
      new VerdictDeReception.Accepte(Optional.empty())
    );
    assertThat(suivi.juge(autreOperateur, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.empty())
    );
  }

  /**
   * La cloture ferme l'activite restee en cours : une fin posterieure a elle n'a plus rien a terminer. Une fin survenue
   * avant, meme recue apres, termine toujours l'activite.
   */
  @Test
  void shouldIgnorerUneFinPosterieureALaClotureEtAccepterUneFinAnterieure() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).cloture(clotureParLeroyA(LE_10_MAI_2026_A_12H));

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_13H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.of(travail.id()))
    );
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_12H)).isEqualTo(
      new VerdictDeReception.Accepte(travail.activite())
    );
    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Accepte(travail.activite())
    );
  }

  /**
   * La cloture ne prolonge jamais une activite echue : une fin posterieure a la cloture reste apres l'echeance.
   */
  @Test
  void shouldGarderLEcheanceDUneActiviteEchueAvantLaCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).cloture(clotureParLeroyA(A_22H));

    assertThat(suivi.juge(cle, TypeDEvenementDAtelier.FIN, A_23H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.APRES_ECHEANCE, Optional.of(travail.id()))
    );
  }
}

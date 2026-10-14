package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La regle de reception juge un pointage sur le journal tel qu'il est apres les corrections du gestionnaire : seuls les
 * gestes bornent ANTERIEUR, les faits d'une activite retiree ne comptent plus, ceux d'une activite dont le poste a
 * change comptent sur le nouveau, et la fin d'un ajout compte comme une fin regularisee.
 */
@UnitTest
class SuiviDAtelierReceptionApresCorrectionsTest {

  private static final Instant LE_10_MAI_2026_A_10H = Instant.parse("2026-05-10T10:00:00Z");
  private static final Instant LE_10_MAI_2026_A_11H = Instant.parse("2026-05-10T11:00:00Z");
  private static final Instant LE_10_MAI_2026_A_14H = Instant.parse("2026-05-10T14:00:00Z");
  private static final Instant LE_10_MAI_2026_A_22H = Instant.parse("2026-05-10T22:00:00Z");

  private final CleDActivite fraiseuse1 = cleDeFraiseuse1DeDupont();
  private final CleDActivite fraiseuse2 = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2));
  private final EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
  private final EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);

  /**
   * Un changement de poste ou un retrait est date du moment de la correction, bien apres les gestes de la cle : s'ils
   * comptaient, ils feraient ignorer tous les pointages hors ligne de la cle jusque-la.
   */
  @Test
  void shouldNePasBornerUnPointageParUneCorrection() {
    EvenementDAtelier autreTravail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_14H);
    EvenementDAtelier autreFin = finDe(autreTravail).a(LE_10_MAI_2026_A_16H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(fin)
      .enregistre(autreTravail)
      .enregistre(autreFin)
      .enregistre(changementDePosteVersFraiseuse2ParLeroyDe(autreTravail).a(LE_11_MAI_2026_A_9H))
      .enregistre(retraitParLeroyDe(travail).a(LE_11_MAI_2026_A_9H.plusSeconds(1)));

    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_9H)).isEqualTo(new VerdictDeReception.Accepte());
    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_17H)).isEqualTo(new VerdictDeReception.Accepte());
  }

  @Test
  void shouldNeJugerQueLeJournalSansLesFaitsDUneActiviteRetiree() {
    SuiviDAtelier avant = suiviDAtelierEngage().enregistre(travail).enregistre(fin);
    SuiviDAtelier apres = avant.enregistre(retraitParLeroyDe(travail).a(LE_11_MAI_2026_A_9H));

    assertThat(avant.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(fin.id()))
    );
    assertThat(apres.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_9H)).isEqualTo(new VerdictDeReception.Accepte());
    assertThat(apres.juge(fraiseuse1, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_9H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.empty())
    );
  }

  /**
   * L'activite qu'un retrait ecarte ne tient plus la cle : un debut pointe pendant ce qui etait une activite en cours
   * n'est plus un double appui.
   */
  @Test
  void shouldAccepterUnDebutPendantUneActiviteRetireeRestEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(retraitParLeroyDe(travail).a(LE_11_MAI_2026_A_9H));

    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_10H)).isEqualTo(new VerdictDeReception.Accepte());
  }

  @Test
  void shouldCompterLesFaitsDUneActiviteSurSonNouveauPoste() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(fin)
      .enregistre(changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_9H));

    assertThat(suivi.juge(fraiseuse2, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_10H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(fin.id()))
    );
    assertThat(suivi.juge(fraiseuse2, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_14H)).isEqualTo(new VerdictDeReception.Accepte());
    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_10H)).isEqualTo(new VerdictDeReception.Accepte());
  }

  /**
   * Une fin automatique dont le poste change reste ouverte sur le nouveau : le geste de l'echeance se juge sur lui.
   */
  @Test
  void shouldJugerLEcheanceDUneFinAutomatiqueSurSonNouveauPoste() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_9H));

    assertThat(suivi.juge(fraiseuse2, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_10H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, Optional.of(travail.id()))
    );
    assertThat(suivi.juge(fraiseuse2, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_22H)).isEqualTo(new VerdictDeReception.Accepte());
    assertThat(suivi.juge(fraiseuse2, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_22H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.APRES_ECHEANCE, Optional.of(travail.id()))
    );
    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_22H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.empty())
    );
  }

  @Test
  void shouldCompterLaFinDUnAjoutCommeUneFinRegularisee() {
    EvenementDAtelier debutAjoute = debutAjouteParLeroySurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier finAjoutee = finRegulariseeParLeroyDe(debutAjoute).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutAjoute).enregistre(finAjoutee);

    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_10H)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(finAjoutee.id()))
    );
    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_12H)).isEqualTo(new VerdictDeReception.Accepte());
  }

  /**
   * Ce que la cloture a termine n'est pas un fait de la cle : un pointage anterieur a la cloture, recu apres elle, reste
   * accepte pour une activite que la cloture a refermee.
   */
  @Test
  void shouldNePasCompterLaClotureCommeUnFait() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).cloture(clotureParLeroyA(LE_10_MAI_2026_A_12H));

    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_10H)).isEqualTo(new VerdictDeReception.Accepte());
  }

  /**
   * Un pointage recu apres un retrait, sur la cle que le retrait a liberee, est ferme par la cloture comme tout autre : le
   * suivi cloture ne garde aucune activite en cours, et une fin posterieure a la cloture est ignoree (ADR 0010).
   */
  @Test
  void shouldFermerParLaClotureUnPointageRecuSurUneCleLiberee() {
    EvenementDAtelier retiree = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier recu = debutSurFraiseuse1ParDupontRecuLe(LE_10_MAI_2026_A_12H.plusSeconds(1800)).a(LE_10_MAI_2026_A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(retiree)
      .enregistre(finDe(retiree).a(LE_10_MAI_2026_A_11H))
      .enregistre(retraitParLeroyDe(retiree).a(LE_10_MAI_2026_A_12H))
      .enregistre(recu)
      .cloture(clotureParLeroyA(LE_10_MAI_2026_A_13H));
    Instant apresLaCloture = LE_10_MAI_2026_A_14H;

    assertThat(suivi.activites())
      .singleElement()
      .satisfies(activite -> assertThat(activite.fin()).contains(LE_10_MAI_2026_A_13H));
    assertThat(suivi.etat(LE_10_MAI_2026_A_16H)).isEqualTo(EtatDAtelier.CLOTURE);
    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_16H)).isEmpty();
    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.FIN, apresLaCloture)).isEqualTo(
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.of(recu.id()))
    );
  }

  @Test
  void shouldFermerParLaClotureUnPointageRecuSurLaCleQuUneActiviteADeplaceeAilleurs() {
    EvenementDAtelier deplacee = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier recu = debutSurFraiseuse1ParDupontRecuLe(LE_10_MAI_2026_A_12H.plusSeconds(1800)).a(LE_10_MAI_2026_A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(deplacee)
      .enregistre(finDe(deplacee).a(LE_10_MAI_2026_A_11H))
      .enregistre(changementDePosteVersFraiseuse2ParLeroyDe(deplacee).a(LE_10_MAI_2026_A_12H))
      .enregistre(recu)
      .cloture(clotureParLeroyA(LE_10_MAI_2026_A_13H));

    assertThat(suivi.activitesEnCours(LE_10_MAI_2026_A_16H)).isEmpty();
    assertThat(suivi.juge(fraiseuse1, TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_14H)).isInstanceOf(VerdictDeReception.Ignore.class);
  }
}

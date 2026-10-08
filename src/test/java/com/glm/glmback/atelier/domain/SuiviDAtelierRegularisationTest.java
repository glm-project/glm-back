package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Ce qu'un suivi exige pour qu'on regularise la fin d'une de ses activites, et la borne qu'il donne a cette fin.
 */
@UnitTest
class SuiviDAtelierRegularisationTest {

  private static final Instant A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant A_22H = Instant.parse("2026-05-10T22:00:00Z");
  private static final Instant A_23H = Instant.parse("2026-05-10T23:00:00Z");

  private final EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
  private final ActiviteId activite = travail.activite().orElseThrow();

  @Test
  void shouldRendreLActiviteEchueQuOnRegularise() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    Activite regularisable = suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_17H, A_22H);

    assertThat(regularisable.ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldRefuserUneActiviteQueLeSuiviNAJamaisOuverte() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);
    ActiviteId inconnue = new ActiviteId(UUID.randomUUID());

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(inconnue, LE_10_MAI_2026_A_17H, A_22H))
      .isExactlyInstanceOf(ActiviteViseeIntrouvableException.class)
      .hasMessageContaining(inconnue.uuid().toString());
  }

  /**
   * L'echeance est atteinte a l'instant meme ou elle tombe : a 21 h pile, une activite commencee a 8 h est echue.
   */
  @Test
  void shouldRefuserUneActiviteTantQuElleNEstPasEchue() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_17H, A_21H.minusNanos(1)))
      .isExactlyInstanceOf(ActiviteNonEchueException.class)
      .hasMessageContaining(activite.uuid().toString());
    assertThat(suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_17H, A_21H).ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldRefuserUneActiviteQuUnPointageADejaTerminee() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_9H, A_22H)).isExactlyInstanceOf(
      ActiviteNonEchueException.class
    );
  }

  @Test
  void shouldRefuserUneActiviteDejaRegularisee() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_17H));

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_16H, A_22H))
      .isExactlyInstanceOf(ActiviteDejaRegulariseeException.class)
      .hasMessageContaining(activite.uuid().toString());
  }

  @Test
  void shouldRegulariserUneActiviteQuandUneAutreActiviteEstDejaRegularisee() {
    EvenementDAtelier suivant = debutSurFraiseuse1ParDupontA(A_22H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(suivant)
      .enregistre(finRegulariseeParLeroyDe(suivant).a(A_23H));

    assertThat(suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_17H, A_23H.plusSeconds(1)).ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldRefuserUneFinFuture() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, A_22H.plusNanos(1), A_22H)).isExactlyInstanceOf(
      DateDeSurvenueFutureException.class
    );
    assertThat(suivi.exigeUneFinRegularisable(activite, A_22H, A_22H).ouvrant()).isEqualTo(travail);
  }

  /**
   * Une fin qui depasse a la fois maintenant et le debut suivant repond qu'elle est future : « maintenant » se juge
   * avant la borne.
   */
  @Test
  void shouldJugerLeFuturAvantLaBorne() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(debutSurFraiseuse1ParDupontA(A_22H));

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, A_23H.plusNanos(1), A_23H)).isExactlyInstanceOf(
      DateDeSurvenueFutureException.class
    );
  }

  @Test
  void shouldRefuserUneFinAvantLeDebutDeLActivite() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_8H.minusNanos(1), A_22H))
      .isExactlyInstanceOf(FinAvantDebutException.class)
      .hasMessageContaining(activite.uuid().toString());
    assertThat(suivi.exigeUneFinRegularisable(activite, LE_10_MAI_2026_A_8H, A_22H).ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldRefuserUneFinApresLeDebutSuivantSurLaCle() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(debutSurFraiseuse1ParDupontA(A_22H));

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, A_22H.plusNanos(1), A_23H))
      .isExactlyInstanceOf(FinApresBorneException.class)
      .hasMessageContaining(activite.uuid().toString());
    assertThat(suivi.exigeUneFinRegularisable(activite, A_22H, A_23H).ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldRefuserUneFinApresLaCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).cloture(clotureParLeroyA(A_22H));

    assertThatThrownBy(() -> suivi.exigeUneFinRegularisable(activite, A_22H.plusNanos(1), A_23H)).isExactlyInstanceOf(
      FinApresBorneException.class
    );
    assertThat(suivi.exigeUneFinRegularisable(activite, A_22H, A_23H).ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldBornerLaFinAuDebutSuivantSurLaCle() {
    EvenementDAtelier suivant = debutSurFraiseuse1ParDupontA(A_22H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(suivant);

    assertThat(suivi.borneDeFin(activite)).contains(A_22H);
    assertThat(suivi.borneDeFin(suivant.activite().orElseThrow())).isEmpty();
  }

  @Test
  void shouldIgnorerLesDebutsDUneAutreCleEtLesFinsPourBornerLaFin() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H))
      .enregistre(debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H))
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.borneDeFin(activite)).isEmpty();
  }

  @Test
  void shouldBornerLaFinALaClotureQuandAucunDebutNeSuit() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).cloture(clotureParLeroyA(A_23H));

    assertThat(suivi.borneDeFin(activite)).contains(A_23H);
  }

  @Test
  void shouldBornerLaFinAuPlusTotDuDebutSuivantEtDeLaCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(debutSurFraiseuse1ParDupontA(A_22H))
      .cloture(clotureParLeroyA(A_23H));

    assertThat(suivi.borneDeFin(activite)).contains(A_22H);
  }

  @Test
  void shouldNeDonnerAucuneBorneSansDebutSuivantNiCloture() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.borneDeFin(activite)).isEqualTo(Optional.empty());
  }
}

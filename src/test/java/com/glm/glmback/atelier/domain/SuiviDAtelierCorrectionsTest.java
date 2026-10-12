package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Ce qu'un suivi exige pour qu'on ajoute une activite, la retire ou change son poste, et ce que ces corrections y
 * laissent : des faits de plus, jamais un fait modifie.
 */
@UnitTest
class SuiviDAtelierCorrectionsTest {

  private static final Instant A_9H = Instant.parse("2026-05-10T09:00:00Z");
  private static final Instant A_10H = Instant.parse("2026-05-10T10:00:00Z");
  private static final Instant A_14H = Instant.parse("2026-05-10T14:00:00Z");
  private static final Instant A_15H = Instant.parse("2026-05-10T15:00:00Z");
  private static final Instant A_22H = Instant.parse("2026-05-10T22:00:00Z");

  private final EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
  private final ActiviteId activite = travail.activite().orElseThrow();
  private final CleDActivite fraiseuse1 = cleDeFraiseuse1DeDupont();

  @Test
  void shouldRendreUneActiviteTermineeParUnePointage() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThat(suivi.exigeUneActiviteCorrigeable(activite, A_22H).ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldRendreUneFinAutomatique() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(suivi.exigeUneActiviteCorrigeable(activite, A_22H).ouvrant()).isEqualTo(travail);
  }

  @Test
  void shouldRefuserUneActiviteEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThatThrownBy(() -> suivi.exigeUneActiviteCorrigeable(activite, A_10H))
      .isExactlyInstanceOf(ActiviteEnCoursException.class)
      .hasMessageContaining(activite.uuid().toString());
  }

  @Test
  void shouldRefuserUneActiviteInconnue() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);
    ActiviteId inconnue = new ActiviteId(UUID.randomUUID());

    assertThatThrownBy(() -> suivi.exigeUneActiviteCorrigeable(inconnue, A_22H)).isExactlyInstanceOf(
      ActiviteViseeIntrouvableException.class
    );
  }

  /**
   * Une activite retiree n'existe plus pour le journal tel qu'on le lit : la retirer encore, ou changer son poste, la
   * trouve introuvable.
   */
  @Test
  void shouldRefuserUneActiviteDejaRetiree() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).retire(retraitParLeroyDe(travail).a(A_22H));

    assertThatThrownBy(() -> suivi.exigeUneActiviteCorrigeable(activite, A_22H)).isExactlyInstanceOf(
      ActiviteViseeIntrouvableException.class
    );
  }

  @Test
  void shouldAccepterUnePlageQuiTouchePlusieursActivitesSansLesRecouper() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .enregistre(debutSurFraiseuse1ParDupontA(A_14H));

    assertThatCode(() ->
      suivi.exigeUnePlageLibre(fraiseuse1, LE_10_MAI_2026_A_12H, A_14H, Optional.empty(), A_22H)
    ).doesNotThrowAnyException();
    assertThatCode(() ->
      suivi.exigeUnePlageLibre(fraiseuse1, LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_8H, Optional.empty(), A_22H)
    ).doesNotThrowAnyException();
  }

  @Test
  void shouldRefuserUnePlageQuiRecoupeUneActiviteDeLaCle() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThatThrownBy(() -> suivi.exigeUnePlageLibre(fraiseuse1, A_10H, A_14H, Optional.empty(), A_22H))
      .isExactlyInstanceOf(ChevauchementException.class)
      .hasMessageContaining(POSTE_ID_FRAISEUSE_1.uuid().toString());
    assertThatThrownBy(() -> suivi.exigeUnePlageLibre(fraiseuse1, LE_10_MAI_2026_A_7H, A_9H, Optional.empty(), A_22H)).isExactlyInstanceOf(
      ChevauchementException.class
    );
    assertThatThrownBy(() -> suivi.exigeUnePlageLibre(fraiseuse1, LE_10_MAI_2026_A_7H, A_14H, Optional.empty(), A_22H)).isExactlyInstanceOf(
      ChevauchementException.class
    );
    assertThatThrownBy(() -> suivi.exigeUnePlageLibre(fraiseuse1, A_9H, A_10H, Optional.empty(), A_22H)).isExactlyInstanceOf(
      ChevauchementException.class
    );
  }

  @Test
  void shouldRefuserUnePlageQuiRecoupeUneFinAutomatiqueJusquaSonEcheance() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThatThrownBy(() ->
      suivi.exigeUnePlageLibre(fraiseuse1, A_15H, Instant.parse("2026-05-10T20:59:59Z"), Optional.empty(), A_22H)
    ).isExactlyInstanceOf(ChevauchementException.class);
    assertThatCode(() ->
      suivi.exigeUnePlageLibre(fraiseuse1, Instant.parse("2026-05-10T21:00:00Z"), A_22H, Optional.empty(), A_22H)
    ).doesNotThrowAnyException();
  }

  @Test
  void shouldRefuserUnePlageQuiRecoupeUneActiviteEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThatThrownBy(() ->
      suivi.exigeUnePlageLibre(fraiseuse1, A_14H, A_15H, Optional.empty(), A_14H.plusSeconds(1))
    ).isExactlyInstanceOf(ChevauchementException.class);
  }

  /**
   * Seule la cle compte : la meme plage sur un autre poste de l'operateur est libre, comme celle d'un autre operateur.
   */
  @Test
  void shouldLaisserLaMemePlageSurUneAutreCle() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));
    CleDActivite fraiseuse2 = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2));
    CleDActivite martin = new CleDActivite(OPERATEUR_ID_MARTIN, Optional.of(POSTE_ID_FRAISEUSE_1));

    assertThatCode(() -> suivi.exigeUnePlageLibre(fraiseuse2, A_9H, A_10H, Optional.empty(), A_22H)).doesNotThrowAnyException();
    assertThatCode(() -> suivi.exigeUnePlageLibre(martin, A_9H, A_10H, Optional.empty(), A_22H)).doesNotThrowAnyException();
  }

  @Test
  void shouldNePasRecouperLActiviteQuOnDeplace() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThatCode(() -> suivi.exigeUnePlageLibre(fraiseuse1, A_9H, A_10H, Optional.of(activite), A_22H)).doesNotThrowAnyException();
  }

  @Test
  void shouldNePasCompterUneActiviteRetireeDansLaPlage() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .retire(retraitParLeroyDe(travail).a(A_22H));

    assertThatCode(() -> suivi.exigeUnePlageLibre(fraiseuse1, A_9H, A_10H, Optional.empty(), A_22H)).doesNotThrowAnyException();
  }

  @Test
  void shouldAjouterUneActiviteParSonOuvertureEtSaFin() {
    EvenementDAtelier ouverture = debutAjouteParLeroySurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finRegulariseeParLeroyDe(ouverture).a(LE_10_MAI_2026_A_12H);

    SuiviDAtelier suivi = suiviDAtelierEngage().ajoute(ouverture, fin, A_22H);

    assertThat(suivi.journal().evenements()).containsExactly(ouverture, fin);
    assertThat(suivi.activites())
      .singleElement()
      .satisfies(ajoutee -> {
        assertThat(ajoutee.ouvrant()).isEqualTo(ouverture);
        assertThat(ajoutee.evenementDeFin()).contains(fin);
      });
  }

  @Test
  void shouldRefuserUnAjoutQuiPrecedeLEngagement() {
    EvenementDAtelier ouverture = debutAjouteParLeroySurFraiseuse1ParDupontA(LE_10_MAI_2026_A_7H.minusSeconds(1));
    EvenementDAtelier fin = finRegulariseeParLeroyDe(ouverture).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThatThrownBy(() -> suivi.ajoute(ouverture, fin, A_22H)).isExactlyInstanceOf(EvenementAvantEngagementException.class);
  }

  @Test
  void shouldRefuserUnAjoutDontLaFinSuitLaCloture() {
    EvenementDAtelier ouverture = debutAjouteParLeroySurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier apres = finRegulariseeParLeroyDe(ouverture).a(LE_10_MAI_2026_A_12H.plusNanos(1));
    EvenementDAtelier pile = finRegulariseeParLeroyDe(ouverture).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier cloture = suiviDAtelierEngage().cloture(clotureParLeroyA(LE_10_MAI_2026_A_12H));

    assertThatThrownBy(() -> cloture.ajoute(ouverture, apres, A_22H))
      .isExactlyInstanceOf(EvenementApresClotureException.class)
      .hasMessageContaining("posterieur a la cloture");
    assertThat(cloture.ajoute(ouverture, pile, A_22H).activites()).hasSize(1);
  }

  @Test
  void shouldRetirerEtChangerLePosteParUnFaitDePlus() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);
    EvenementDAtelier retrait = retraitParLeroyDe(travail).a(A_22H);
    EvenementDAtelier changement = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(A_22H);

    assertThat(suivi.retire(retrait).journal().evenements()).containsExactlyInAnyOrder(travail, fin, retrait);
    assertThat(suivi.changeLePoste(changement, A_22H).journal().evenements()).containsExactlyInAnyOrder(travail, fin, changement);
    assertThat(suivi.changeLePoste(changement, A_22H).activites()).singleElement().extracting(Activite::porteur).isEqualTo(changement);
  }

  /**
   * Un retrait ou un changement de poste est date du moment de la correction : sur un suivi cloture avant cet instant,
   * il suivrait la cloture et le journal n'a pas le droit de le porter.
   */
  @Test
  void shouldRefuserUneCorrectionPosterieureALaCloture() {
    SuiviDAtelier cloture = suiviDAtelierEngage().enregistre(travail).cloture(clotureParLeroyA(LE_10_MAI_2026_A_12H));
    EvenementDAtelier retrait = retraitParLeroyDe(travail).a(A_22H);
    EvenementDAtelier changement = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(A_22H);

    assertThatThrownBy(() -> cloture.retire(retrait)).isExactlyInstanceOf(EvenementApresClotureException.class);
    assertThatThrownBy(() -> cloture.changeLePoste(changement, A_22H)).isExactlyInstanceOf(EvenementApresClotureException.class);
    assertThat(cloture.retire(retraitParLeroyDe(travail).a(LE_10_MAI_2026_A_12H)).activites()).isEmpty();
  }

  /**
   * Une activite dont le poste a change reste a regulariser : le changement de poste n'est pas une regularisation de sa
   * fin.
   */
  @Test
  void shouldRegulariserLaFinDUneActiviteDontLePosteAChange() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(changementDePosteVersFraiseuse2ParLeroyDe(travail).a(A_9H));

    assertThat(suivi.exigeUneFinRegularisable(activite, A_10H, A_22H).cle().poste()).contains(POSTE_ID_FRAISEUSE_2);
  }

  @Test
  void shouldLireLesGestesDeLaCleApresLesCorrections() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier autre = debutSurFraiseuse1ParDupontA(A_14H);
    EvenementDAtelier finDeLAutre = finDe(autre).a(A_15H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin).enregistre(autre).enregistre(finDeLAutre);
    CleDActivite fraiseuse2 = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2));

    assertThat(suivi.gestesDeLaCle(fraiseuse1)).containsExactly(travail, fin, autre, finDeLAutre);
    assertThat(suivi.retire(retraitParLeroyDe(travail).a(A_22H)).gestesDeLaCle(fraiseuse1)).containsExactly(autre, finDeLAutre);
    SuiviDAtelier change = suivi.changeLePoste(changementDePosteVersFraiseuse2ParLeroyDe(travail).a(A_22H), A_22H);
    assertThat(change.gestesDeLaCle(fraiseuse1)).containsExactly(autre, finDeLAutre);
    assertThat(change.gestesDeLaCle(fraiseuse2)).containsExactly(travail, fin);
  }

  @Test
  void shouldRefuserUnAjoutQuiChevaucheUneActiviteDeLaCle() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));
    EvenementDAtelier ouverture = debutAjouteParLeroySurFraiseuse1ParDupontA(A_10H);
    EvenementDAtelier fin = finRegulariseeParLeroyDe(ouverture).a(A_14H);

    assertThatThrownBy(() -> suivi.ajoute(ouverture, fin, A_22H)).isExactlyInstanceOf(ChevauchementException.class);
  }

  /**
   * La cloture se verifie avant le chevauchement : un ajout qui suit la cloture est refuse pour cela, quelle que soit la
   * place qu'il prendrait.
   */
  @Test
  void shouldRefuserLaClotureAvantLeChevauchement() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .cloture(clotureParLeroyA(A_14H));
    EvenementDAtelier ouverture = debutAjouteParLeroySurFraiseuse1ParDupontA(A_10H);
    EvenementDAtelier fin = finRegulariseeParLeroyDe(ouverture).a(A_15H);

    assertThatThrownBy(() -> suivi.ajoute(ouverture, fin, A_22H)).isExactlyInstanceOf(EvenementApresClotureException.class);
  }

  @Test
  void shouldRefuserUnChangementDePosteQuiChevaucheLeNouveauPoste() {
    EvenementDAtelier surFraiseuse2 = debutSurFraiseuse2ParDupontA(A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .enregistre(surFraiseuse2)
      .enregistre(finDe(surFraiseuse2).a(A_10H));
    EvenementDAtelier changement = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(A_22H);

    assertThatThrownBy(() -> suivi.changeLePoste(changement, A_22H)).isExactlyInstanceOf(ChevauchementException.class);
  }

  @Test
  void shouldRefuserUnChangementDePosteSurUneActiviteEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);
    EvenementDAtelier changement = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(A_10H);

    assertThatThrownBy(() -> suivi.changeLePoste(changement, A_10H)).isExactlyInstanceOf(ActiviteEnCoursException.class);
  }
}

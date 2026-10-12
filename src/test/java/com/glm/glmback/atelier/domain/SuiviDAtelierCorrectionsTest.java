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
  void shouldAccepterUnAjoutQuiTouchePlusieursActivitesSansLesRecouper() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .enregistre(debutSurFraiseuse1ParDupontA(A_14H));

    assertThatCode(() -> ajoute(suivi, fraiseuse1, LE_10_MAI_2026_A_12H, A_14H)).doesNotThrowAnyException();
    assertThatCode(() -> ajoute(suivi, fraiseuse1, LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_8H)).doesNotThrowAnyException();
  }

  @Test
  void shouldRefuserUnAjoutQuiRecoupeUneActiviteDeLaCle() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThatThrownBy(() -> ajoute(suivi, fraiseuse1, A_10H, A_14H))
      .isExactlyInstanceOf(ChevauchementException.class)
      .hasMessageContaining(POSTE_ID_FRAISEUSE_1.uuid().toString());
    assertThatThrownBy(() -> ajoute(suivi, fraiseuse1, LE_10_MAI_2026_A_7H, A_9H)).isExactlyInstanceOf(ChevauchementException.class);
    assertThatThrownBy(() -> ajoute(suivi, fraiseuse1, LE_10_MAI_2026_A_7H, A_14H)).isExactlyInstanceOf(ChevauchementException.class);
    assertThatThrownBy(() -> ajoute(suivi, fraiseuse1, A_9H, A_10H)).isExactlyInstanceOf(ChevauchementException.class);
  }

  @Test
  void shouldRefuserUnAjoutQuiRecoupeUneFinAutomatiqueJusquaSonEcheance() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    assertThatThrownBy(() -> ajoute(suivi, fraiseuse1, A_15H, Instant.parse("2026-05-10T20:59:59Z"))).isExactlyInstanceOf(
      ChevauchementException.class
    );
    assertThatCode(() -> ajoute(suivi, fraiseuse1, Instant.parse("2026-05-10T21:00:00Z"), A_22H)).doesNotThrowAnyException();
  }

  @Test
  void shouldRefuserUnAjoutQuiRecoupeUneActiviteEnCours() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);
    EvenementDAtelier ouverture = ouverture(fraiseuse1, A_10H);

    assertThatThrownBy(() -> suivi.ajoute(ouverture, fin(ouverture, A_14H), A_14H)).isExactlyInstanceOf(ChevauchementException.class);
  }

  /**
   * Seule la cle compte : la meme plage sur un autre poste de l'operateur est libre, comme celle d'un autre operateur.
   */
  @Test
  void shouldLaisserLaMemePlageSurUneAutreCle() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H));
    CleDActivite fraiseuse2 = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2));
    CleDActivite martin = new CleDActivite(OPERATEUR_ID_MARTIN, Optional.of(POSTE_ID_FRAISEUSE_1));

    assertThatCode(() -> ajoute(suivi, fraiseuse2, A_9H, A_10H)).doesNotThrowAnyException();
    assertThatCode(() -> ajoute(suivi, martin, A_9H, A_10H)).doesNotThrowAnyException();
  }

  /**
   * L'activite qu'on deplace ne se recoupe pas elle-meme : la ramener sur sa propre cle n'est pas un chevauchement.
   */
  @Test
  void shouldNePasRecouperLActiviteQuOnDeplace() {
    EvenementDAtelier surFraiseuse2 = debutSurFraiseuse2ParDupontA(A_9H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(surFraiseuse2).enregistre(finDe(surFraiseuse2).a(A_10H));

    assertThatCode(() ->
      suivi.changeLePoste(changementDePosteVersFraiseuse2ParLeroyDe(surFraiseuse2).a(A_22H), A_22H)
    ).doesNotThrowAnyException();
  }

  @Test
  void shouldNePasCompterUneActiviteRetireeDansUnAjout() {
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
      .retire(retraitParLeroyDe(travail).a(A_22H));

    assertThatCode(() -> ajoute(suivi, fraiseuse1, A_9H, A_10H)).doesNotThrowAnyException();
  }

  @Test
  void shouldRefuserUneFinQuiNeSuitPasLeDebut() {
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThatThrownBy(() -> ajoute(suivi, fraiseuse1, A_10H, A_10H)).isExactlyInstanceOf(FinAvantDebutException.class);
    assertThatThrownBy(() -> ajoute(suivi, fraiseuse1, A_10H, A_9H)).isExactlyInstanceOf(FinAvantDebutException.class);
  }

  @Test
  void shouldRefuserUneFinDuFutur() {
    SuiviDAtelier suivi = suiviDAtelierEngage();
    EvenementDAtelier ouverture = ouverture(fraiseuse1, A_9H);
    EvenementDAtelier fin = fin(ouverture, A_10H);

    assertThatThrownBy(() -> suivi.ajoute(ouverture, fin, A_10H.minusNanos(1))).isExactlyInstanceOf(DateDeSurvenueFutureException.class);
    assertThatCode(() -> suivi.ajoute(ouverture, fin, A_10H)).doesNotThrowAnyException();
  }

  /**
   * Le suivi protege l'ajout de son appelant : une fin qui ne ferme pas l'ouverture donnee (autre type, autre activite,
   * autre cle) ou une ouverture qui n'en est pas une n'est jamais ecrite.
   */
  @Test
  void shouldRefuserUnAjoutIncoherent() {
    SuiviDAtelier suivi = suiviDAtelierEngage();
    EvenementDAtelier ouverture = ouverture(fraiseuse1, A_9H);
    EvenementDAtelier autreOuverture = ouverture(fraiseuse1, A_9H);

    assertThatThrownBy(() -> suivi.ajoute(finDe(travail).a(A_10H), fin(fraiseuse1, A_9H, A_10H), A_22H)).isExactlyInstanceOf(
      EvenementDAtelierIncoherentException.class
    );
    assertThatThrownBy(() -> suivi.ajoute(ouverture, retraitParLeroyDe(ouverture).a(A_10H), A_22H)).isExactlyInstanceOf(
      EvenementDAtelierIncoherentException.class
    );
    EvenementDAtelier pointee = debutSurFraiseuse1ParDupontA(A_9H);
    assertThatThrownBy(() -> suivi.ajoute(pointee, finRegulariseeParLeroyDe(pointee).a(A_10H), A_22H)).isExactlyInstanceOf(
      EvenementDAtelierIncoherentException.class
    );
    assertThatThrownBy(() -> suivi.ajoute(ouverture, finPointeeDe(ouverture).a(A_10H), A_22H)).isExactlyInstanceOf(
      EvenementDAtelierIncoherentException.class
    );
    assertThatThrownBy(() -> suivi.ajoute(ouverture, fin(autreOuverture, A_10H), A_22H)).isExactlyInstanceOf(
      EvenementDAtelierIncoherentException.class
    );
    assertThatThrownBy(() -> suivi.ajoute(ouverture, finRegulariseeSurFraiseuse2ParLeroyDe(ouverture).a(A_10H), A_22H)).isExactlyInstanceOf(
      EvenementDAtelierIncoherentException.class
    );
  }

  /**
   * La regle de l'ADR 0015 : une activite dont la fin a ete regularisee, ou qu'on a ajoutee, se corrige comme une autre.
   */
  @Test
  void shouldCorrigerUneActiviteDontLaFinEstRegularisee() {
    EvenementDAtelier regularisee = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(regularisee);

    assertThat(suivi.exigeUneActiviteCorrigeable(activite, A_22H).evenementDeFin()).contains(regularisee);
    assertThat(suivi.changeLePoste(changementDePosteVersFraiseuse2ParLeroyDe(travail).a(A_22H), A_22H).activites())
      .singleElement()
      .satisfies(changee -> {
        assertThat(changee.cle().poste()).contains(POSTE_ID_FRAISEUSE_2);
        assertThat(changee.fin()).contains(LE_10_MAI_2026_A_17H);
      });
    assertThat(suivi.retire(retraitParLeroyDe(travail).a(A_22H)).activites()).isEmpty();
  }

  @Test
  void shouldCorrigerUneActiviteAjoutee() {
    SuiviDAtelier suivi = ajoute(suiviDAtelierEngage(), fraiseuse1, A_9H, A_10H);
    ActiviteId ajoutee = suivi.activites().getFirst().id();

    assertThat(suivi.exigeUneActiviteCorrigeable(ajoutee, A_22H).fin()).contains(A_10H);
    assertThat(
      suivi.changeLePoste(changementDePosteVersFraiseuse2ParLeroyDe(suivi.activites().getFirst().ouvrant()).a(A_22H), A_22H).activites()
    )
      .singleElement()
      .satisfies(changee -> {
        assertThat(changee.cle().poste()).contains(POSTE_ID_FRAISEUSE_2);
        assertThat(changee.fin()).contains(A_10H);
      });
    assertThat(suivi.retire(retraitParLeroyDe(suivi.activites().getFirst().ouvrant()).a(A_22H)).activites()).isEmpty();
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

  private static EvenementDAtelier ouverture(CleDActivite cle, Instant debut) {
    return debutAjouteParLeroySur(cle).a(debut);
  }

  private static EvenementDAtelier fin(CleDActivite cle, Instant debut, Instant fin) {
    return fin(ouverture(cle, debut), fin);
  }

  private static EvenementDAtelier fin(EvenementDAtelier ouverture, Instant fin) {
    return finAjouteeParLeroyDe(ouverture).a(fin);
  }

  /**
   * Ajoute une activite a cette cle, de debut a fin, un 10 mai a 22 h.
   */
  private static SuiviDAtelier ajoute(SuiviDAtelier suivi, CleDActivite cle, Instant debut, Instant fin) {
    EvenementDAtelier ouverture = ouverture(cle, debut);

    return suivi.ajoute(ouverture, fin(ouverture, fin), A_22H);
  }
}

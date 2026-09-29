package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class TempsDAtelierServiceTest {

  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_17H);
  private final AtomicReference<AmplitudeMaximale> seuil = new AtomicReference<>(AMPLITUDE_MAXIMALE_13H);
  private final TempsDAtelierService temps = TempsDAtelierService.builder().suivis(suivis).journees(journees).seuil(seuil::get);

  @Test
  void shouldNotReadTempsEffectifDUnSuiviInconnu() {
    SuiviDAtelierId inconnu = SuiviDAtelierId.newId();

    assertThatThrownBy(() -> temps.tempsEffectif(inconnu, maintenant.get())).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  /**
   * La pause de midi est pointee dans le journal de l'element, par une fin et un debut : la presence, d'un seul tenant
   * de l'arrivee au depart, n'y retranche rien.
   */
  @Test
  void shouldScinderLeTravailALaPauseDeMidiPointeeSurLElement() {
    journees.create(journeeDeDupontDe7HA17H());
    EvenementDAtelier reprise = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    SuiviDAtelierId suivi = enAtelier(avecPauseDeMidiPuis(reprise).enregistre(finDe(reprise).a(LE_10_MAI_2026_A_17H)));

    assertThat(temps.tempsEffectif(suivi, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  /**
   * L'operateur oublie d'arreter son element et rentre chez lui : son depart referme l'activite, la ou un intervalle
   * brut aurait couru indefiniment.
   */
  @Test
  void shouldRefermerAuDepartUneActiviteJamaisArretee() {
    journees.create(journeeDeDupontDe7HA17H());
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H)));

    assertThat(temps.tempsEffectif(suivi, maintenant.get()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_13H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
      });
  }

  /**
   * Une seule regularisation de depart corrige tous les elements de la journee, la ou un depart recopie element par
   * element aurait demande autant de corrections que d'elements.
   */
  @Test
  void shouldRefermerTousLesElementsSurUneSeuleRegularisationDeDepart() {
    JourneeDeTravail ouverte = journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId premier = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    SuiviDAtelierId second = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H)));

    journees.update(ouverte.enregistre(departRegulariseParLeroyA(LE_10_MAI_2026_A_17H)));

    assertThat(temps.tempsEffectif(premier, maintenant.get()))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
    assertThat(temps.tempsEffectif(second, maintenant.get()))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldLaisserOuvertUnTravailEnCoursSurUneJourneeEnCours() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(temps.tempsEffectif(suivi, maintenant.get())).singleElement().matches(IntervalleDActivite::estOuvert);
  }

  /**
   * Le domaine ne masque pas l'anomalie : sans presence saisie, l'intervalle brut est rendu tel quel, et c'est la
   * presence qui reste a regulariser.
   */
  @Test
  void shouldRendreIntactUnIntervalleSansAucunePresenceConnue() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_17H)));

    assertThat(temps.tempsEffectif(suivi, maintenant.get()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
      });
  }

  /**
   * Un travail commence a l'instant du depart ne tombe dans aucune fenetre de presence : il ne compte rien.
   */
  @Test
  void shouldEcarterUnTravailEntierementHorsDesFenetresDePresence() {
    journees.create(journeeDeDupontDe7HA17H());
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H);
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_20H)));

    assertThat(temps.tempsEffectif(suivi, maintenant.get())).isEmpty();
  }

  @Test
  void shouldNeRienAjouterAuTempsEffectifSurUnDoubleAppui() {
    journees.create(journeeDeDupontDe7HA17H());
    EvenementDAtelier secondAppui = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H.plusSeconds(3));
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(secondAppui)
        .enregistre(finDe(secondAppui).a(LE_10_MAI_2026_A_12H))
    );

    assertThat(temps.tempsEffectif(suivi, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_8H.plusSeconds(3))),
        tuple(LE_10_MAI_2026_A_8H.plusSeconds(3), Optional.of(LE_10_MAI_2026_A_12H))
      );
  }

  /**
   * E2 : Dupont part lundi sans rien pointer. Lu mardi, l'OF 42 s'arrete a la fin presumee de lundi, la fin de l'OF 43
   * a 16:00 : 7 h, la nuit n'est plus comptee. Sans depart, lundi n'a qu'une fenetre de presence, et ses 7 h sont
   * toutes presumees.
   */
  @Test
  void shouldArreterUnTravailALaFinPresumeeDUneJourneeAbandonnee() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId of42 = enAtelier(avecPauseDeMidi());
    enAtelier(of43De9HA16H());
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(of42, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), true),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H), true)
      );
  }

  /**
   * E2 complet : la relance de mardi 07:05 tombe hors de la fenetre de recherche de lundi, et compte dans mardi.
   */
  @Test
  void shouldSeparerLundiPresumeDeLaRelanceDeMardi() {
    journees.create(journeeDeDupontOuverteA7H());
    journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_DUPONT).enregistre(arriveeDeDupontA(LE_11_MAI_2026_A_7H))
    );
    SuiviDAtelierId of42 = enAtelier(avecPauseDeMidi().enregistre(debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_7H.plusSeconds(300))));
    enAtelier(of43De9HA16H());
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(of42, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), true),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H), true),
        tuple(LE_11_MAI_2026_A_7H.plusSeconds(300), Optional.empty(), false)
      );
  }

  @Test
  void shouldNePasCompterLeTravailDApresLaFinPresumee() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId nuit = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_3H)));
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(nuit, maintenant.get())).isEmpty();
  }

  @Test
  void shouldIgnorerLesPointagesDUnAutreOperateur() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId of42 = enAtelier(avecPauseDeMidi());
    enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_17H)));
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(of42, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)));
  }

  @Test
  void shouldRemplacerLePresumeParLePointeApresRegularisation() {
    journees.create(journeeDeDupontOuverteA7H().enregistre(departRegulariseParLeroyA(LE_10_MAI_2026_A_17H)));
    SuiviDAtelierId of42 = enAtelier(avecPauseDeMidi());
    maintenant.set(LE_11_MAI_2026_A_9H15);

    assertThat(temps.tempsEffectif(of42, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H), false)
      );
  }

  @Test
  void shouldLaisserOuvertUnTravailDUneJourneeNonAbandonnee() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_10_MAI_2026_A_20H);

    assertThat(temps.tempsEffectif(of42, maintenant.get()))
      .last()
      .satisfies(intervalle -> {
        assertThat(intervalle.fin()).isEmpty();
        assertThat(intervalle.presume()).isFalse();
      });
  }

  /**
   * E8 : un seuil ramene a 10 h arrete la recherche a 17:00. La fin de l'OF 43 a 18:00 n'est plus un fait de lundi, et
   * l'OF 42 s'arrete au debut de l'OF 43, a 9:00.
   */
  @Test
  void shouldChercherLaFinPresumeeDansLeSeuilCourant() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    EvenementDAtelier debutDeLOf43 = debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H);
    enAtelier(suiviDAtelierEngage().enregistre(debutDeLOf43).enregistre(finDe(debutDeLOf43).a(LE_10_MAI_2026_A_17H.plusSeconds(3600))));
    maintenant.set(LE_11_MAI_2026_A_9H);
    seuil.set(AMPLITUDE_MAXIMALE_10H);

    assertThat(temps.tempsEffectif(of42, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_9H), true));
  }

  /**
   * E3 : le poste de nuit oublie ne laisse que son arrivee et un debut d'OF cinq minutes plus tard. L'OF ne recoit
   * aucune heure : la fin presumee n'invente rien.
   */
  @Test
  void shouldNInventerAucuneHeureAUnPosteDeNuitOublie() {
    journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_DUPONT).enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_20H))
    );
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_20H.plusSeconds(300))));
    maintenant.set(LE_11_MAI_2026_A_20H);

    assertThat(temps.tempsEffectif(of42, maintenant.get())).isEmpty();
  }

  /**
   * Issue #59 : un OF jamais arrete, commence dans une journee fermee de plus de 24 h, s'arrete a la fin presumee de
   * celle-ci, la fin de l'OF 43 a 16:00 — et non au depart du lendemain.
   */
  @Test
  void shouldArreterUnTravailALaFinPresumeeDUneJourneeDePlusDe24H() {
    journees.create(journeeDeDupontDu10A7HAu11A9H());
    SuiviDAtelierId of42 = enAtelier(avecPauseDeMidi());
    enAtelier(of43De9HA16H());
    maintenant.set(LE_11_MAI_2026_A_9H15);

    assertThat(temps.tempsEffectif(of42, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), true),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H), true)
      );
  }

  /**
   * Sans presence connue, l'activite que rien n'a terminee est rendue a sa fin automatique, a 21 h, avec son anomalie.
   */
  @Test
  void shouldRendreSaFinAutomatiqueAUneActiviteEchueSansPresence() {
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(suivi, maintenant.get()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.fin()).contains(Instant.parse("2026-05-10T21:00:00Z"));
        assertThat(intervalle.finAutomatique()).isTrue();
      });
  }

  /**
   * L'anomalie suit l'activite : ramenee a la presence de sa journee, l'activite terminee automatiquement le reste.
   */
  @Test
  void shouldSignalerLaFinAutomatiqueDUneActiviteRameneeALaPresence() {
    journees.create(journeeDeDupontDe7HA17H());
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H)));
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(suivi, maintenant.get()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
        assertThat(intervalle.finAutomatique()).isTrue();
      });
  }

  /**
   * L'OF 42 de Dupont sur la fraiseuse 1, avec la pause de midi telle que le pupitre la pointe : un debut a 8 h, sa fin
   * a 12 h, un debut a 13 h.
   */
  /**
   * Une activite a resoudre n'a aucune duree a presenter comme definitive : la presence ne lui donne pas de fin, et son
   * intervalle est rendu tel quel, sans fin, signale a resoudre.
   */
  @Test
  void shouldRendreSansFinUneActiviteAResoudreMemeRameneeALaPresence() {
    journees.create(journeeDeDupontDe7HA17H());
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(premiere)
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H))
        .enregistre(finDe(premiere).a(LE_10_MAI_2026_A_12H))
    );

    assertThat(temps.tempsEffectif(suivi, maintenant.get()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::aResoudre)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, Optional.empty(), true), tuple(LE_10_MAI_2026_A_9H, Optional.empty(), true));
  }

  private static SuiviDAtelier avecPauseDeMidi() {
    return avecPauseDeMidiPuis(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H));
  }

  private static SuiviDAtelier avecPauseDeMidiPuis(EvenementDAtelier reprise) {
    EvenementDAtelier matin = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    return suiviDAtelierEngage().enregistre(matin).enregistre(finDe(matin).a(LE_10_MAI_2026_A_12H)).enregistre(reprise);
  }

  /**
   * L'OF 43 de Dupont sur la fraiseuse 2, de 9 h a 16 h.
   */
  private static SuiviDAtelier of43De9HA16H() {
    EvenementDAtelier debut = debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H);

    return suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_16H));
  }

  private SuiviDAtelierId enAtelier(SuiviDAtelier suivi) {
    return suivis.create(suivi).id();
  }
}

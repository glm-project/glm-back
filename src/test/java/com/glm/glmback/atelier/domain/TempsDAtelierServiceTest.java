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
  private final TempsDAtelierService temps = TempsDAtelierService.builder()
    .suivis(suivis)
    .journees(journees)
    .seuil(seuil::get)
    .clock(maintenant::get);

  @Test
  void shouldNotReadTempsEffectifDUnSuiviInconnu() {
    SuiviDAtelierId inconnu = SuiviDAtelierId.newId();

    assertThatThrownBy(() -> temps.tempsEffectif(inconnu)).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  /**
   * La pause de midi n'est jamais entree dans le journal de l'element : elle le scinde pourtant en deux, parce que le
   * temps effectif est l'intersection des deux journaux.
   */
  @Test
  void shouldScinderLeTravailAutourDeLaPauseDeMidi() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H))
    );

    assertThat(temps.tempsEffectif(suivi))
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
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H)));

    assertThat(temps.tempsEffectif(suivi))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_13H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
      });
  }

  /**
   * Une seule regularisation de depart corrige tous les elements de la journee, la ou une pause recopiee element par
   * element aurait demande autant de corrections que d'elements.
   */
  @Test
  void shouldRefermerTousLesElementsSurUneSeuleRegularisationDeDepart() {
    JourneeDeTravail ouverte = journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId premier = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    SuiviDAtelierId second = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H)));

    journees.update(ouverte.enregistre(departRegulariseParLeroyA(LE_10_MAI_2026_A_17H)));

    assertThat(temps.tempsEffectif(premier))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
    assertThat(temps.tempsEffectif(second))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldLaisserOuvertUnTravailEnCoursSurUneJourneeEnCours() {
    journees.create(journeeDeDupontOuverteA7H());
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(temps.tempsEffectif(suivi)).singleElement().matches(IntervalleDActivite::estOuvert);
  }

  /**
   * Le domaine ne masque pas l'anomalie : sans presence saisie, l'intervalle brut est rendu tel quel, et c'est la
   * presence qui reste a regulariser.
   */
  @Test
  void shouldRendreIntactUnIntervalleSansAucunePresenceConnue() {
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H))
    );

    assertThat(temps.tempsEffectif(suivi))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
      });
  }

  @Test
  void shouldEcarterUnTravailEntierementHorsDesFenetresDePresence() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    assertThat(temps.tempsEffectif(suivi)).isEmpty();
  }

  /**
   * Une relance pointee pendant la pause ne fait pas compter la pause : le temps effectif reste l'intersection avec la
   * presence.
   */
  @Test
  void shouldAmputerDeLaPauseUneActiviteRelanceePendantLaPause() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H.plusSeconds(1800)))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_17H))
    );

    assertThat(temps.tempsEffectif(suivi))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  @Test
  void shouldNeRienAjouterAuTempsEffectifSurUnDoubleAppui() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H.plusSeconds(3)))
        .enregistre(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H))
    );

    assertThat(temps.tempsEffectif(suivi))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_8H.plusSeconds(3))),
        tuple(LE_10_MAI_2026_A_8H.plusSeconds(3), Optional.of(LE_10_MAI_2026_A_12H))
      );
  }

  /**
   * E2 : Dupont part lundi sans rien pointer. Lu mardi, l'OF 42 s'arrete a la fin presumee de lundi, la fin de l'OF 43
   * a 16:00 : 4 h pointees et 3 h presumees, la nuit n'est plus comptee.
   */
  @Test
  void shouldArreterUnTravailALaFinPresumeeDUneJourneeAbandonnee() {
    journees.create(journeeDeLundiSansDepart());
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H))
        .enregistre(finSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_16H))
    );
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(of42))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H), true)
      );
  }

  /**
   * E2 complet : la relance de mardi 07:05 tombe hors de la fenetre de recherche de lundi, et compte dans mardi.
   */
  @Test
  void shouldSeparerLundiPresumeDeLaRelanceDeMardi() {
    journees.create(journeeDeLundiSansDepart());
    journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_DUPONT).enregistre(arriveeDeDupontA(LE_11_MAI_2026_A_7H))
    );
    SuiviDAtelierId of42 = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_7H.plusSeconds(300)))
    );
    enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H))
        .enregistre(finSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_16H))
    );
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(of42))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H), true),
        tuple(LE_11_MAI_2026_A_7H.plusSeconds(300), Optional.empty(), false)
      );
  }

  @Test
  void shouldNePasCompterLeTravailDApresLaFinPresumee() {
    journees.create(journeeDeLundiSansDepart());
    SuiviDAtelierId nuit = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_3H)));
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(nuit)).isEmpty();
  }

  @Test
  void shouldIgnorerLesPointagesDUnAutreOperateur() {
    journees.create(journeeDeLundiSansDepart());
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_17H)));
    maintenant.set(LE_11_MAI_2026_A_9H);

    assertThat(temps.tempsEffectif(of42))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)));
  }

  @Test
  void shouldRemplacerLePresumeParLePointeApresRegularisation() {
    journees.create(journeeDeLundiSansDepart().enregistre(departRegulariseParLeroyA(LE_10_MAI_2026_A_17H)));
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_11_MAI_2026_A_9H15);

    assertThat(temps.tempsEffectif(of42))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::presume)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H), false)
      );
  }

  @Test
  void shouldLaisserOuvertUnTravailDUneJourneeNonAbandonnee() {
    journees.create(journeeDeLundiSansDepart());
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_10_MAI_2026_A_20H);

    assertThat(temps.tempsEffectif(of42))
      .last()
      .satisfies(intervalle -> {
        assertThat(intervalle.fin()).isEmpty();
        assertThat(intervalle.presume()).isFalse();
      });
  }

  /**
   * E8 : un seuil ramene a 10 h arrete la recherche a 17:00. La fin de l'OF 43 a 18:00 n'est plus un fait de lundi.
   */
  @Test
  void shouldChercherLaFinPresumeeDansLeSeuilCourant() {
    journees.create(journeeDeLundiSansDepart());
    SuiviDAtelierId of42 = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));
    enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H))
        .enregistre(finSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_17H.plusSeconds(3600)))
    );
    maintenant.set(LE_11_MAI_2026_A_9H);
    seuil.set(AMPLITUDE_MAXIMALE_10H);

    assertThat(temps.tempsEffectif(of42))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::presume)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, false));
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

    assertThat(temps.tempsEffectif(of42)).isEmpty();
  }

  private static JourneeDeTravail journeeDeLundiSansDepart() {
    return journeeDeDupontOuverteA7H().enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H)).enregistre(repriseDeDupontA(LE_10_MAI_2026_A_13H));
  }

  private SuiviDAtelierId enAtelier(SuiviDAtelier suivi) {
    return suivis.create(suivi).id();
  }
}

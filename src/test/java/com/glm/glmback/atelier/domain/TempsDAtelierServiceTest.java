package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class TempsDAtelierServiceTest {

  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final TempsDAtelierService temps = new TempsDAtelierService(suivis);

  @Test
  void shouldNotReadTempsEffectifDUnSuiviInconnu() {
    SuiviDAtelierId inconnu = SuiviDAtelierId.newId();

    assertThatThrownBy(() -> temps.tempsEffectif(inconnu, LE_10_MAI_2026_A_17H)).isExactlyInstanceOf(
      SuiviDAtelierIntrouvableException.class
    );
  }

  /**
   * Releve d'un intervalle 08:00-10:00 : l'activite compte entre son debut et sa fin.
   */
  @Test
  void shouldRendreLIntervalleDUneActiviteTermineeSansAucunePresence() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    Instant a10h = LE_10_MAI_2026_A_8H.plusSeconds(7200);
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(a10h)));

    assertThat(temps.tempsEffectif(suivi, LE_10_MAI_2026_A_17H))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::finAutomatique)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, Optional.of(a10h), false));
  }

  /**
   * La pause de midi est pointee dans le journal de l'element, par une fin et un debut : elle scinde le travail.
   */
  @Test
  void shouldScinderLeTravailALaPauseDeMidiPointeeSurLElement() {
    EvenementDAtelier reprise = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    SuiviDAtelierId suivi = enAtelier(avecPauseDeMidiPuis(reprise).enregistre(finDe(reprise).a(LE_10_MAI_2026_A_17H)));

    assertThat(temps.tempsEffectif(suivi, LE_10_MAI_2026_A_17H))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  @Test
  void shouldLaisserOuvertUnTravailEncoreEnCours() {
    SuiviDAtelierId suivi = enAtelier(suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(temps.tempsEffectif(suivi, LE_10_MAI_2026_A_17H)).singleElement().matches(IntervalleDActivite::estOuvert);
  }

  @Test
  void shouldNeRienAjouterAuTempsEffectifSurUnDoubleAppui() {
    EvenementDAtelier secondAppui = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H.plusSeconds(3));
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
        .enregistre(secondAppui)
        .enregistre(finDe(secondAppui).a(LE_10_MAI_2026_A_12H))
    );

    assertThat(temps.tempsEffectif(suivi, LE_10_MAI_2026_A_17H))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_8H.plusSeconds(3))),
        tuple(LE_10_MAI_2026_A_8H.plusSeconds(3), Optional.of(LE_10_MAI_2026_A_12H))
      );
  }

  /**
   * Le travail repris lundi a 13 h reste sans fin pointee : lu mardi, il est termine automatiquement a son echeance,
   * 13 heures apres son debut, avec son anomalie.
   */
  @Test
  void shouldTerminerAutomatiquementASonEcheanceUnTravailJamaisArrete() {
    SuiviDAtelierId suivi = enAtelier(avecPauseDeMidi());

    assertThat(temps.tempsEffectif(suivi, LE_11_MAI_2026_A_9H))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::finAutomatique)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(LE_10_MAI_2026_A_13H, Optional.of(Instant.parse("2026-05-11T02:00:00Z")), true)
      );
  }

  /**
   * Une activite a resoudre n'a aucune duree a presenter comme definitive : son intervalle est rendu sans fin, signale a
   * resoudre.
   */
  @Test
  void shouldRendreSansFinUneActiviteAResoudre() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelierId suivi = enAtelier(
      suiviDAtelierEngage()
        .enregistre(premiere)
        .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H))
        .enregistre(finDe(premiere).a(LE_10_MAI_2026_A_12H))
    );

    assertThat(temps.tempsEffectif(suivi, LE_10_MAI_2026_A_17H))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::aResoudre)
      .containsExactly(tuple(LE_10_MAI_2026_A_8H, Optional.empty(), true), tuple(LE_10_MAI_2026_A_9H, Optional.empty(), true));
  }

  /**
   * L'OF 42 de Dupont sur la fraiseuse 1, avec la pause de midi telle que le pupitre la pointe : un debut a 8 h, sa fin
   * a 12 h, un debut a 13 h.
   */
  private static SuiviDAtelier avecPauseDeMidi() {
    return avecPauseDeMidiPuis(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H));
  }

  private static SuiviDAtelier avecPauseDeMidiPuis(EvenementDAtelier reprise) {
    EvenementDAtelier matin = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    return suiviDAtelierEngage().enregistre(matin).enregistre(finDe(matin).a(LE_10_MAI_2026_A_12H)).enregistre(reprise);
  }

  private SuiviDAtelierId enAtelier(SuiviDAtelier suivi) {
    return suivis.create(suivi).id();
  }
}

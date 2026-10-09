package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * La lecture du journal suppose qu'au plus une activite est en cours sur une cle : la regle de reception le garantit.
 * Ces suites de pointages, jouees par {@code juge} puis {@code enregistre} comme le fait le service, en pointant parfois
 * dans le desordre ou apres l'echeance, ne la prennent jamais en defaut.
 */
@UnitTest
class SuiviDAtelierAuPlusUneActiviteParCleTest {

  private static final EvenementDAtelier DONNEUR_DE_CLE = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_7H);
  private static final TypeDEvenementDAtelier[] TYPES = TypeDEvenementDAtelier.values();

  @Test
  void shouldNeLaisserQuUneActiviteVivanteParCleSurDesSuitesDePointagesAcceptesParLaRegle() {
    int acceptes = 0;
    for (long graine = 0; graine < 300; graine++) {
      Random hasard = new Random(graine);
      SuiviDAtelier suivi = suiviDAtelierEngage();
      Instant heure = LE_10_MAI_2026_A_8H;

      for (int pas = 0; pas < 40; pas++) {
        heure = heure.plusSeconds((hasard.nextInt(14 * 60) - 120) * 60L);
        TypeDEvenementDAtelier type = TYPES[hasard.nextInt(TYPES.length)];
        if (
          heure.isBefore(LE_10_MAI_2026_A_7H) || !(suivi.juge(cleDeFraiseuse1DeDupont(), type, heure) instanceof VerdictDeReception.Accepte)
        ) {
          continue;
        }
        suivi = suivi.enregistre(pointage(type, heure));
        acceptes++;

        for (Instant lecture : List.of(heure, heure.plusSeconds(3600), heure.plusSeconds(14 * 3600))) {
          assertThat(suivi.activitesEnCours(lecture))
            .describedAs("graine %s, pas %s, lecture %s", graine, pas, lecture)
            .hasSizeLessThanOrEqualTo(1);
        }
      }
    }
    assertThat(acceptes).describedAs("pointages acceptes").isGreaterThan(1000);
  }

  private static EvenementDAtelier pointage(TypeDEvenementDAtelier type, Instant heure) {
    return switch (type) {
      case DEBUT -> debutSurFraiseuse1ParDupontA(heure);
      case NON_CONFORMITE -> nonConformiteSurFraiseuse1ParDupontA(heure);
      case FIN -> finDe(DONNEUR_DE_CLE).a(heure);
    };
  }
}

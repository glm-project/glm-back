package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

@UnitTest
class EcheanceTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Echeance(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("echeance");
  }

  /**
   * Une activite commencee a 8 h qui ne recoit aucune fin se termine automatiquement a 21 h.
   */
  @Test
  void shouldTomberTreizeHeuresApresLeDebut() {
    assertThat(Echeance.apres(LE_10_MAI_2026_A_8H, DUREE_MAXIMALE_TREIZE_HEURES).value()).isEqualTo(Instant.parse("2026-05-10T21:00:00Z"));
  }

  /**
   * La duree vient de l'activite, qui la tient du reglage en vigueur a son debut : fixee a huit heures par le
   * gestionnaire, l'echeance d'une activite commencee a 8 h tombe a 16 h.
   */
  @Test
  void shouldTomberLaDureeParametreeApresLeDebut() {
    assertThat(Echeance.apres(LE_10_MAI_2026_A_8H, DUREE_MAXIMALE_HUIT_HEURES).value()).isEqualTo(Instant.parse("2026-05-10T16:00:00Z"));
  }

  /**
   * Treize heures ecoulees, pas treize heures d'horloge murale : la nuit du passage a l'heure d'ete, a Paris, une
   * activite commencee a 1 h 30 se termine automatiquement a 15 h 30, quatorze heures plus tard au cadran.
   */
  @Test
  void shouldCompterDesHeuresEcouleesAuChangementDHeure() {
    ZoneId paris = ZoneId.of("Europe/Paris");
    Instant debut = LocalDateTime.parse("2026-03-29T01:30:00").atZone(paris).toInstant();

    Instant echeance = Echeance.apres(debut, DUREE_MAXIMALE_TREIZE_HEURES).value();

    assertThat(echeance.atZone(paris).toLocalDateTime()).isEqualTo(LocalDateTime.parse("2026-03-29T15:30:00"));
  }
}

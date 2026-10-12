package com.glm.glmback.atelier.domain.correctiondesactivites;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class DecoupageParJourTest {

  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private final DecoupageParJour decoupage = new DecoupageParJour(PARIS);

  @Test
  void shouldNotBuildWithoutFuseau() {
    assertThatThrownBy(() -> new DecoupageParJour(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fuseau horaire");
  }

  @Test
  void shouldPlacerLesMinuitsDansLeFuseauDeLEntreprise() {
    assertThat(decoupage.debutDe(LocalDate.parse("2026-10-05"))).isEqualTo(Instant.parse("2026-10-04T22:00:00Z"));
    assertThat(decoupage.debutDe(LocalDate.parse("2026-12-05"))).isEqualTo(Instant.parse("2026-12-04T23:00:00Z"));
  }

  @Test
  void shouldGarderUnePlageDUnSeulJourEntiere() {
    List<PortionDeJour> portions = decoupage.portions(Instant.parse("2026-10-05T07:00:00Z"), Instant.parse("2026-10-05T10:30:00Z"));

    assertThat(portions).containsExactly(new PortionDeJour(LocalDate.parse("2026-10-05"), Duration.ofMinutes(210)));
  }

  @Test
  void shouldCouperUnePlageAMinuit() {
    List<PortionDeJour> portions = decoupage.portions(Instant.parse("2026-10-05T20:00:00Z"), Instant.parse("2026-10-06T01:30:00Z"));

    assertThat(portions).containsExactly(
      new PortionDeJour(LocalDate.parse("2026-10-05"), Duration.ofHours(2)),
      new PortionDeJour(LocalDate.parse("2026-10-06"), Duration.ofMinutes(210))
    );
  }

  @Test
  void shouldCouperUnePlageAChaqueMinuitQuElleTraverse() {
    List<PortionDeJour> portions = decoupage.portions(Instant.parse("2026-10-05T20:00:00Z"), Instant.parse("2026-10-07T22:00:00Z"));

    assertThat(portions)
      .extracting(PortionDeJour::jour)
      .containsExactly(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-06"), LocalDate.parse("2026-10-07"));
    assertThat(portions).extracting(PortionDeJour::duree).containsExactly(Duration.ofHours(2), Duration.ofHours(24), Duration.ofHours(24));
  }

  @Test
  void shouldNeRendreAucunePortionPourUnePlageVide() {
    Instant heure = Instant.parse("2026-10-05T07:00:00Z");

    assertThat(decoupage.portions(heure, heure)).isEmpty();
    assertThat(decoupage.portions(heure, heure.minusSeconds(1))).isEmpty();
  }

  /**
   * La nuit du passage a l'heure d'hiver dure vingt-cinq heures : une portion est un ecart reel entre deux instants. De
   * 22 h a 3 h 30 la nuit du 24 au 25 octobre, il s'ecoule deux heures avant minuit puis quatre heures et demie.
   */
  @Test
  void shouldCompterLaDureeReelleDeLaNuitDuChangementDHeure() {
    Instant debut = Instant.parse("2026-10-24T22:00:00+02:00");
    Instant fin = Instant.parse("2026-10-25T03:30:00+01:00");

    assertThat(decoupage.portions(debut, fin)).containsExactly(
      new PortionDeJour(LocalDate.parse("2026-10-24"), Duration.ofHours(2)),
      new PortionDeJour(LocalDate.parse("2026-10-25"), Duration.ofMinutes(270))
    );
  }
}

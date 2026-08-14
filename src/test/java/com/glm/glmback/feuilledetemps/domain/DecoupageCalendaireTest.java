package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La semaine 20 de 2026 va du lundi 11 au dimanche 17 mai, en heure d'ete : minuit a Paris, c'est 22h UTC la veille.
 * C'est tout l'interet du decoupage, donc les heures restent ici en clair.
 */
@UnitTest
class DecoupageCalendaireTest {

  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final SemaineCalendaire SEMAINE_20_DE_2026 = new SemaineCalendaire(2026, 20);

  @Test
  void shouldNotBuildWithoutSemaine() {
    assertThatThrownBy(() -> new DecoupageCalendaire(null, PARIS))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("semaine");
  }

  @Test
  void shouldNotBuildWithoutZone() {
    assertThatThrownBy(() -> new DecoupageCalendaire(SEMAINE_20_DE_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fuseau horaire");
  }

  @Test
  void shouldPorterLesSeptJoursDuLundiAuDimanche() {
    assertThat(decoupage().jours()).containsExactly(
      LocalDate.of(2026, 5, 11),
      LocalDate.of(2026, 5, 12),
      LocalDate.of(2026, 5, 13),
      LocalDate.of(2026, 5, 14),
      LocalDate.of(2026, 5, 15),
      LocalDate.of(2026, 5, 16),
      LocalDate.of(2026, 5, 17)
    );
  }

  @Test
  void shouldCommencerEtFinirAMinuitDansLaZoneDeLEntreprise() {
    assertThat(decoupage().debut()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(decoupage().finExclusive()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  @Test
  void shouldGarderEntiereUnePlageTenantDansUnSeulJour() {
    List<PlageDUnJour> plages = decoupage().plages(fermee(le(11, 8), le(11, 12)));

    assertThat(plages).hasSize(1);
    assertThat(plages.getFirst().jour()).isEqualTo(LocalDate.of(2026, 5, 11));
    assertThat(plages.getFirst().plage().debut()).isEqualTo(le(11, 8));
    assertThat(plages.getFirst().plage().fin()).contains(le(11, 12));
  }

  @Test
  void shouldScinderUnePlageAChevalSurMinuit() {
    List<PlageDUnJour> plages = decoupage().plages(fermee(le(11, 22), le(12, 2)));

    assertThat(plages).hasSize(2);
    assertThat(plages.getFirst().jour()).isEqualTo(LocalDate.of(2026, 5, 11));
    assertThat(plages.getFirst().plage().fin()).contains(le(12, 0));
    assertThat(plages.getLast().jour()).isEqualTo(LocalDate.of(2026, 5, 12));
    assertThat(plages.getLast().plage().debut()).isEqualTo(le(12, 0));
    assertThat(plages.getLast().plage().fin()).contains(le(12, 2));
  }

  @Test
  void shouldRendreUnePlageParJourTraverse() {
    List<PlageDUnJour> plages = decoupage().plages(fermee(le(11, 22), le(14, 2)));

    assertThat(plages)
      .extracting(PlageDUnJour::jour)
      .containsExactly(LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 13), LocalDate.of(2026, 5, 14));
  }

  @Test
  void shouldCouperUnePlageCommenceeAvantLaSemaine() {
    List<PlageDUnJour> plages = decoupage().plages(fermee(le(10, 20), le(11, 6)));

    assertThat(plages).hasSize(1);
    assertThat(plages.getFirst().jour()).isEqualTo(LocalDate.of(2026, 5, 11));
    assertThat(plages.getFirst().plage().debut()).isEqualTo(decoupage().debut());
  }

  @Test
  void shouldCouperUnePlageFinissantApresLaSemaine() {
    List<PlageDUnJour> plages = decoupage().plages(fermee(le(17, 20), le(18, 6)));

    assertThat(plages).hasSize(1);
    assertThat(plages.getFirst().jour()).isEqualTo(LocalDate.of(2026, 5, 17));
    assertThat(plages.getFirst().plage().fin()).contains(decoupage().finExclusive());
  }

  @Test
  void shouldIgnorerUnePlageEntierementAvantLaSemaine() {
    assertThat(decoupage().plages(fermee(le(9, 8), le(9, 12)))).isEmpty();
  }

  @Test
  void shouldIgnorerUnePlageEntierementApresLaSemaine() {
    assertThat(decoupage().plages(fermee(le(18, 8), le(18, 12)))).isEmpty();
  }

  /**
   * Une plage ouverte ne court pas jusqu'a la fin de la semaine : sans depart, rien ne dit que l'operateur etait la
   * le lendemain, et l'atelier applique deja la meme regle a un travail jamais arrete.
   */
  @Test
  void shouldRendreUnePlageOuverteSurSonSeulJour() {
    List<PlageDUnJour> plages = decoupage().plages(new Plage(le(13, 8), Optional.empty()));

    assertThat(plages).hasSize(1);
    assertThat(plages.getFirst().jour()).isEqualTo(LocalDate.of(2026, 5, 13));
    assertThat(plages.getFirst().plage().fin()).isEmpty();
  }

  @Test
  void shouldIgnorerUnePlageOuverteCommenceeAvantLaSemaine() {
    assertThat(decoupage().plages(new Plage(le(9, 8), Optional.empty()))).isEmpty();
  }

  private static DecoupageCalendaire decoupage() {
    return new DecoupageCalendaire(SEMAINE_20_DE_2026, PARIS);
  }

  private static Plage fermee(Instant debut, Instant fin) {
    return new Plage(debut, Optional.of(fin));
  }

  private static Instant le(int jourDeMai, int heure) {
    return LocalDateTime.of(2026, 5, jourDeMai, heure, 0).atZone(PARIS).toInstant();
  }
}

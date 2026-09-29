package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import com.glm.glmback.shared.error.domain.TooManyElementsException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteTest {

  @Test
  void shouldNotBuildWithoutOuvrant() {
    assertThatThrownBy(() -> new Activite(null, Optional.empty(), false))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("ouvrant");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> new Activite(ouvrant, null, false))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotEndBeforeItsDebut() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    Optional<Instant> avantLeDebut = Optional.of(LE_10_MAI_2026_A_7H);

    assertThatThrownBy(() -> new Activite(ouvrant, avantLeDebut, false))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildAResoudreAvecUneFin() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    Optional<Instant> fin = Optional.of(LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> new Activite(ouvrant, fin, true))
      .isExactlyInstanceOf(TooManyElementsException.class)
      .hasMessageContaining("fin d'une activite a resoudre");
  }

  /**
   * Une activite prise dans une sequence en conflit perd sa fin : les pointages contradictoires ne permettent plus de
   * l'affirmer.
   */
  @Test
  void shouldPerdreSaFinDansUneSequenceEnConflit() {
    Activite terminee = Activite.ouvertePar(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)).termineeA(LE_10_MAI_2026_A_12H);

    Activite enConflit = terminee.enConflit();

    assertThat(enConflit.aResoudre()).isTrue();
    assertThat(enConflit.fin()).isEmpty();
    assertThat(enConflit.ouvrant()).isEqualTo(terminee.ouvrant());
  }

  /**
   * Une activite a resoudre n'est ni en cours ni terminee, et son echeance ne la termine pas : lue avant comme apres,
   * elle n'a ni fin ni anomalie de fin automatique.
   */
  @Test
  void shouldSeLireAResoudreAvantCommeApresSonEcheance() {
    Activite aResoudre = Activite.ouvertePar(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)).enConflit();

    assertThat(List.of(LE_10_MAI_2026_A_9H, LE_11_MAI_2026_A_9H15)).allSatisfy(lecture -> {
      assertThat(aResoudre.estEnCoursA(lecture)).isFalse();
      assertThat(aResoudre.a(lecture)).satisfies(lue -> {
        assertThat(lue.aResoudre()).isTrue();
        assertThat(lue.fin()).isEmpty();
        assertThat(lue.finAutomatique()).isFalse();
      });
    });
  }

  /**
   * Une activite porte l'identite de son pointage ouvrant d'origine, et tient de lui sa cle, sa categorie et son
   * debut.
   */
  @Test
  void shouldTenirDeSonOuvrantSonIdentiteSaCleSaCategorieEtSonDebut() {
    EvenementDAtelier ouvrant = nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    Activite activite = Activite.ouvertePar(ouvrant);

    assertThat(activite.id()).isEqualTo(ActiviteId.ouvertePar(ouvrant.id()));
    assertThat(activite.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
    assertThat(activite.categorie()).isEqualTo(CategorieDActivite.NON_CONFORMITE);
    assertThat(activite.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(activite.fin()).isEmpty();
    assertThat(activite.aResoudre()).isFalse();
  }

  @Test
  void shouldGarderSonOuvrantUneFoisTerminee() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    Activite terminee = Activite.ouvertePar(ouvrant).termineeA(LE_10_MAI_2026_A_12H);

    assertThat(terminee.ouvrant()).isEqualTo(ouvrant);
    assertThat(terminee.fin()).contains(LE_10_MAI_2026_A_12H);
  }

  @Test
  void shouldEcheoirTreizeHeuresApresSonDebut() {
    Activite activite = Activite.ouvertePar(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    assertThat(activite.echeance()).isEqualTo(Echeance.apres(LE_10_MAI_2026_A_8H));
  }

  /**
   * Activite a 08:00, lecture a 20:59 : elle est encore en cours, sans fin ni anomalie.
   */
  @Test
  void shouldEtreEnCoursAvantSonEcheance() {
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    IntervalleDActivite lue = Activite.ouvertePar(ouvrant).a(Instant.parse("2026-05-10T20:59:00Z"));

    assertThat(lue.estOuvert()).isTrue();
    assertThat(lue.finAutomatique()).isFalse();
    assertThat(lue.evenement()).isEqualTo(ouvrant.id());
    assertThat(lue.activite()).isEqualTo(ActiviteId.ouvertePar(ouvrant.id()));
    assertThat(lue.cle()).isEqualTo(cleDeFraiseuse1DeDupont());
    assertThat(lue.nature()).contains(NATURE_FRAISAGE);
    assertThat(lue.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
    assertThat(lue.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
  }

  /**
   * Activite a 08:00, aucune fin a 21:00 : elle est terminee automatiquement a son echeance, avec une anomalie.
   */
  @Test
  void shouldSeTerminerAutomatiquementASonEcheance() {
    Activite activite = Activite.ouvertePar(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    IntervalleDActivite lue = activite.a(Instant.parse("2026-05-10T21:00:00Z"));

    assertThat(lue.fin()).contains(Instant.parse("2026-05-10T21:00:00Z"));
    assertThat(lue.finAutomatique()).isTrue();
  }

  /**
   * Lue le lendemain, elle garde la meme borne : l'echeance, pas l'instant de la lecture.
   */
  @Test
  void shouldGarderSaFinAutomatiqueALaLectureDuLendemain() {
    Activite activite = Activite.ouvertePar(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    IntervalleDActivite lue = activite.a(LE_11_MAI_2026_A_9H15);

    assertThat(lue.fin()).contains(Instant.parse("2026-05-10T21:00:00Z"));
    assertThat(lue.finAutomatique()).isTrue();
  }

  /**
   * Une fin reelle ne depend pas de l'instant de lecture, et ne laisse aucune anomalie, meme au-dela de l'echeance.
   */
  @Test
  void shouldSeLireASaFinReelleQuelQueSoitLInstant() {
    Activite terminee = Activite.ouvertePar(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)).termineeA(LE_10_MAI_2026_A_12H);

    assertThat(List.of(LE_10_MAI_2026_A_9H, LE_11_MAI_2026_A_9H15))
      .map(terminee::a)
      .allSatisfy(lue -> {
        assertThat(lue.fin()).contains(LE_10_MAI_2026_A_12H);
        assertThat(lue.finAutomatique()).isFalse();
      });
  }
}

package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JournalDAtelierDiagnosticTest {

  @Test
  void shouldExpliquerLaFinDUneActiviteRemplaceeParLaNonConformite() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_17H);
    JournalDAtelier journal = new JournalDAtelier(List.of(travail, nonConformite, finDuTravail));

    assertThat(journal.diagnostics(Optional.empty())).containsExactly(
      new DiagnosticDeConflit(
        finDuTravail.id(),
        new CibleDuConflit(travail.activite().orElseThrow(), Optional.of(travail.id()), Optional.of(nonConformite.id())),
        RaisonDuConflit.CIBLE_REMPLACEE
      )
    );
  }

  @Test
  void shouldDesignerLaPremiereFinQuandUneSecondeFinLaContredit() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier premiereFin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier secondeFin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    JournalDAtelier journal = new JournalDAtelier(List.of(travail, premiereFin, secondeFin));

    assertThat(journal.diagnostics(Optional.empty()))
      .singleElement()
      .satisfies(diagnostic -> {
        assertThat(diagnostic.pointage()).isEqualTo(secondeFin.id());
        assertThat(diagnostic.raison().name()).isEqualTo("CIBLE_DEJA_TERMINEE");
        assertThat(diagnostic.cible().termineePar()).contains(premiereFin.id());
      });
  }

  @Test
  void shouldDesignerLOuvertureQuandUneFinLaPrecede() {
    var ouvrant = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(ouvrant).a(LE_10_MAI_2026_A_7H);
    var journal = new JournalDAtelier(List.of(ouvrant, fin));

    assertThat(journal.diagnostics(Optional.empty()))
      .singleElement()
      .satisfies(diagnostic -> {
        assertThat(diagnostic.raison().name()).isEqualTo("GESTE_AVANT_OUVERTURE");
        assertThat(diagnostic.cible().ouvrant()).contains(ouvrant.id());
        assertThat(diagnostic.cible().termineePar()).isEmpty();
      });
  }

  @Test
  void shouldExpliquerLaFinOrphelineSansInventerDActivite() {
    var ouvrant = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(ouvrant).a(LE_10_MAI_2026_A_17H);
    var journal = new JournalDAtelier(List.of(ouvrant, fin)).annule(ouvrant.id(), annulationParLeroy());

    assertThat(journal.diagnostics(Optional.empty()))
      .singleElement()
      .satisfies(diagnostic -> {
        assertThat(diagnostic.raison().name()).isEqualTo("OUVRANT_ANNULE");
        assertThat(diagnostic.cible().ouvrant()).contains(ouvrant.id());
      });
    assertThat(journal.activites(Optional.empty())).isEmpty();
  }

  @Test
  void shouldExpliquerUneTransitionQuiReprendLaMemeCategorie() {
    var ouvrant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnTravailDe(ouvrant).a(LE_10_MAI_2026_A_12H);
    var journal = new JournalDAtelier(List.of(ouvrant, transition));

    assertThat(journal.diagnostics(Optional.empty()))
      .singleElement()
      .satisfies(diagnostic -> {
        assertThat(diagnostic.raison().name()).isEqualTo("TRANSITION_MEME_CATEGORIE");
        assertThat(diagnostic.pointage()).isEqualTo(transition.id());
        assertThat(diagnostic.cible().ouvrant()).contains(ouvrant.id());
      });
  }

  @Test
  void shouldDesignerLeRemplacementAvantLaCategorieDUneTransitionTardive() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var reprise = passageEnTravailDe(travail).a(java.time.Instant.parse("2026-05-10T14:00:00Z"));
    var journal = new JournalDAtelier(List.of(travail, nonConformite, reprise));

    assertThat(journal.diagnostics(Optional.empty()))
      .singleElement()
      .satisfies(diagnostic -> {
        assertThat(diagnostic.raison()).isEqualTo(RaisonDuConflit.CIBLE_REMPLACEE);
        assertThat(diagnostic.cible().termineePar()).contains(nonConformite.id());
      });
  }

  @Test
  void shouldDistinguerUneCibleEchueDuTravailQuiLaSuit() {
    var a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var b = debutSurFraiseuse1ParDupontA(java.time.Instant.parse("2026-05-10T22:00:00Z"));
    var transition = passageEnNonConformiteDe(a).a(java.time.Instant.parse("2026-05-10T23:00:00Z"));
    var journal = new JournalDAtelier(List.of(a, b, transition));

    assertThat(journal.diagnostics(Optional.empty()))
      .singleElement()
      .satisfies(diagnostic -> {
        assertThat(diagnostic.raison().name()).isEqualTo("CIBLE_ECHUE_AVEC_AUTRE_ACTIVITE");
        assertThat(diagnostic.cible().ouvrant()).contains(a.id());
        assertThat(diagnostic.cible().termineePar()).isEmpty();
      });
  }

  @Test
  void shouldExpliquerUneProlongationRegulariseeEtUneFinRegulariseeApresRelance() {
    var a = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnNonConformiteDe(a).a(java.time.Instant.parse("2026-05-10T22:00:00Z"));
    var fin = finRegulariseeParLeroyDe(a).a(java.time.Instant.parse("2026-05-10T23:00:00Z"));
    var journal = new JournalDAtelier(List.of(a, transition, fin));

    assertThat(journal.diagnostics(Optional.empty()))
      .extracting(diagnostic -> diagnostic.raison().name())
      .containsExactly("CONTRADICTION_REGULARISATION", "CONTRADICTION_REGULARISATION");

    var relance = debutSurFraiseuse1ParDupontA(java.time.Instant.parse("2026-05-10T22:00:00Z"));
    var apresRelance = new JournalDAtelier(List.of(a, relance, fin));
    assertThat(apresRelance.diagnostics(Optional.empty()))
      .singleElement()
      .satisfies(diagnostic -> {
        assertThat(diagnostic.raison().name()).isEqualTo("CONTRADICTION_REGULARISATION");
        assertThat(diagnostic.cible().termineePar()).contains(relance.id());
      });
  }
}

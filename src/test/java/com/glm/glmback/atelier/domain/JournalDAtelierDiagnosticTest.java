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
}
